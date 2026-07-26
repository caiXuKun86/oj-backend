package com.kun.oj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.model.dto.question.*;
import com.kun.oj.model.entity.*;
import com.kun.oj.service.*;
import com.qcloud.cos.model.PutObjectResult;
import com.kun.oj.common.DeleteRequest;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.manage.cos.CosManager;
import com.kun.oj.mapper.QuestionBaseMapper;

import com.kun.oj.model.enums.JudgeStatusEnum;
import com.kun.oj.model.enums.LanguageEnum;
import com.kun.oj.model.enums.QuestionDifficultyEnum;
import com.kun.oj.model.vo.QuestionBaseVO;
import com.kun.oj.model.vo.QuestionVO;
import com.kun.oj.utils.RedisCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Lenovo
 * @description 针对表【question(题目)】的数据库操作Service实现
 * @createDate 2026-06-12 21:58:20
 */

@Slf4j
@Service
public class QuestionBaseServiceImpl extends ServiceImpl<QuestionBaseMapper, QuestionBase>
        implements QuestionBaseService {
    @Resource
    private CosManager cosManager;
    @Resource
    private UserService userService;
    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionTagRelatedService questionTagRelatedService;
    @Resource
    private QuestionTagService questionTagService;
    @Resource
    private QuestionBaseMapper questionBaseMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Lazy
    private JudgeService judgeService;

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private QuestionExtentService questionExtentService;


    @Override
    public Long submitQuestion(QuestionSubmitRequest questionSubmitRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionSubmitRequest == null, ErrorCode.PARAMS_ERROR);
        String language = questionSubmitRequest.getLanguage();
        String code = questionSubmitRequest.getCode();
        Long questionId = questionSubmitRequest.getQuestionId();

        ThrowUtils.throwIf(ObjUtil.hasNull(language, questionId), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.PARAMS_ERROR);
        LanguageEnum languageEnum = LanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目前不支持该语言");
        }
        if (code.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码内容过长");
        }
        QuestionBase question = this.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        RLock lock = redissonClient.getLock("lock:question:submit:" + questionId + ":" + userId);
        try {
            boolean b = lock.tryLock();
            if (!b) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复提交");
            }
            boolean exists = questionSubmitService.lambdaQuery()
                    .eq(QuestionSubmit::getQuestionId, questionId)
                    .eq(QuestionSubmit::getUserId, userId)
                    .in(QuestionSubmit::getStatus, JudgeStatusEnum.WAITING.getValue(), JudgeStatusEnum.RUNNING.getValue())
                    .exists();
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "你已有待处理的提交记录,请勿重复提交");
            QuestionSubmit questionSubmit = new QuestionSubmit();
            questionSubmit.setLanguage(language);
            questionSubmit.setCode(code);
            questionSubmit.setJudgeInfo("{}");
            questionSubmit.setStatus(JudgeStatusEnum.WAITING.getValue());
            questionSubmit.setQuestionId(questionId);
            questionSubmit.setUserId(userId);
            questionSubmit.setNumber(question.getNumber());
            questionSubmit.setTitle(question.getTitle());
            questionSubmit.setUserAccount(loginUser.getUserAccount());

            boolean result = questionSubmitService.save(questionSubmit);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "提交失败");
            judgeService.doJudge(questionSubmit.getId());
            return questionSubmit.getId();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }

    @Value("${oj.path.testcaseUpload}")
    private String testcaseUploadPath;

    @Override
    public TestCaseUploadResponse uploadTestCases(MultipartFile multipartFile, Long questionId) {
        // 定义全局基础路径
        String globalPath = testcaseUploadPath + File.separator + questionId;

        // 0.检查文件类型 (注意：Hutool 提取的后缀不带点，且最好忽略大小写)
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (suffix == null || !"zip".equalsIgnoreCase(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只支持 zip 文件");
        }

        try {
            // 1. 定义路径
            String zipFileName = UUID.randomUUID().toString(true) + ".zip";
            String zipFilePath = globalPath + File.separator + zipFileName;
            File zipFile = FileUtil.file(zipFilePath);

            // 优化：独立的解压目录名，更优雅
            String unzipDirPath = globalPath + File.separator + "unzip";
            File unzipDir = FileUtil.file(unzipDirPath);

            // 修复：确保父级目录存在，否则 transferTo 会报错
            FileUtil.mkParentDirs(zipFile);

            try {
                multipartFile.transferTo(zipFile);
            } catch (IOException e) {
                log.error("文件保存到本地临时目录失败, {}", e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败");
            }

            // 解压文件
            ZipUtil.unzip(zipFile, unzipDir, StandardCharsets.UTF_8);

            // 2. 检查用例是否合规，统计用例数量
            List<File> files = FileUtil.loopFiles(unzipDir);
            if (CollUtil.isEmpty(files) || files.size() % 2 != 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例为空或未成对");
            }

            Set<String> fileNameSet = new HashSet<>();
            for (File file : files) {
                fileNameSet.add(file.getName());
            }

            for (File file : files) {
                String fileName = file.getName();
                // 修复：获取后缀（不带点）
                String caseSuffix = FileUtil.getSuffix(fileName);
                String mainName = FileUtil.mainName(fileName);

                // 如果不是 in 或 out 文件，或者混入了类似 readme.txt 的东西
                if (!"in".equalsIgnoreCase(caseSuffix) && !"out".equalsIgnoreCase(caseSuffix)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例包含非法文件: " + fileName);
                }

                // 严格检查成对情况：只要是 in 文件，必须有对应的 out 文件
                if ("in".equalsIgnoreCase(caseSuffix)) {
                    if (!fileNameSet.contains(mainName + ".out")) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例缺失输出文件: " + mainName + ".out");
                    }
                }
            }

            // 3. 上传到 COS
            String key = "testcase/" + questionId + "/" + zipFileName;
            PutObjectResult putObjectResult = null;
            try {
                putObjectResult = cosManager.putObject(key, zipFile);
            } catch (Exception e) {
                log.error("文件上传到COS失败, {}", e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传到存储服务器失败");
            }

            // 返回 COS Key 和用例组数
            return new TestCaseUploadResponse(key, files.size() / 2, putObjectResult.getETag());

        } catch (BusinessException e) {
            // BusinessException 直接往外抛，保留原有的错误信息提示给前端
            throw e;
        } catch (Exception e) {
            log.error("处理测试用例失败, {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理失败");

        } finally {
            // 4. 清理临时文件 (兜底策略，放在 finally 中确保一定执行)
            FileUtil.del(globalPath);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addQuestion(QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        //添加题目基础信息
        QuestionBase questionBase = new QuestionBase();
        BeanUtils.copyProperties(questionAddRequest, questionBase);
        User loginUser = userService.getLoginUser(request);
        questionBase.setUserId(loginUser.getId());
        this.validate(questionBase, null);
        boolean result = this.save(questionBase);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //添加题目扩展信息
        Long questionId = questionBase.getId();
        QuestionExtent questionExtent = new QuestionExtent();
        BeanUtil.copyProperties(questionAddRequest, questionExtent);
        questionExtent.setId(questionId);
        this.validate(null, questionExtent);
        boolean save = questionExtentService.save(questionExtent);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        //添加题目标签信息
        List<Long> tagIds = questionAddRequest.getTagIds();
        if (CollUtil.isNotEmpty(tagIds)) {
            List<QuestionTagRelated> questionTagRelatedList = tagIds.stream()
                    .map(questionTagId -> {
                        QuestionTagRelated related = new QuestionTagRelated();
                        related.setQuestionId(questionBase.getId());
                        related.setQuestionTagId(questionTagId);
                        return related;
                    }).collect(Collectors.toList());


            boolean batchResult = questionTagRelatedService.saveBatch(questionTagRelatedList);
            ThrowUtils.throwIf(!batchResult, ErrorCode.OPERATION_ERROR);
        }


        return questionBase.getId();
    }

    @Override
    public QuestionVO getQuestionVOById(Long id) {
        String cacheKey = "cache:questionItem:" + id;
        String cacheStr = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheStr != null) {
            if (StrUtil.isBlank(cacheStr)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
            }
            return JSONUtil.toBean(cacheStr, QuestionVO.class);
        }
        String lockKey = "lock:cache:question" + id;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean b = lock.tryLock(2, TimeUnit.SECONDS);
            if (!b) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询失败");
            }
            // 双重检查：拿到锁后最好再查一次缓存，可能前一个抢到锁的线程已经写入了缓存
            cacheStr = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheStr != null) {
                if (StrUtil.isBlank(cacheStr)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
                }
                return JSONUtil.toBean(cacheStr, QuestionVO.class);
            }
            QuestionBase questionBase = this.getById(id);
            if (questionBase == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, "", 30, TimeUnit.SECONDS);
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
            }
            QuestionExtent questionExtent = questionExtentService.getById(id);
            List<Long> tagIds = questionTagRelatedService.lambdaQuery()
                    .select(QuestionTagRelated::getQuestionTagId)
                    .eq(QuestionTagRelated::getQuestionId, id)
                    .list()
                    .stream()
                    .map(QuestionTagRelated::getQuestionTagId)
                    .collect(Collectors.toList());
            List<QuestionTag> questionTags = null;
            if (CollUtil.isNotEmpty(tagIds)) {
                questionTags = questionTagService.listByIds(tagIds);
            }
            QuestionVO questionVO = new QuestionVO();
            BeanUtil.copyProperties(questionBase, questionVO);
            questionVO.setContent(questionExtent.getContent());
            String sampleJudgeCase = questionExtent.getSampleJudgeCase();
            String hiddenJudgeCase = questionExtent.getHiddenJudgeCase();
            questionVO.setSampleJudgeCaseList(JSONUtil.toList(sampleJudgeCase, SampleJudgeCase.class));
            questionVO.setHiddenJudgeCase(JSONUtil.toBean(hiddenJudgeCase, HiddenJudgeCase.class));
            questionVO.setTags(questionTags);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(questionVO), 10 + RandomUtil.randomInt(-2, 2), TimeUnit.MINUTES);
            return questionVO;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }

    @Override
    public Page<QuestionBaseVO> listQuestionVOByPage(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        int current = questionQueryRequest.getCurrent();
        int pageSize = questionQueryRequest.getPageSize();
        Page<QuestionBase> page = new Page<>(current, pageSize);

        String hash = questionQueryRequest.generateCacheKeyHash();
        String cacheKey = "cache:questionPage:" + hash;
        String cacheStr = stringRedisTemplate.opsForValue().get(cacheKey);
        List<QuestionBaseVO> questionVOList = null;

        if (StrUtil.isBlank(cacheStr)) {
            page = questionBaseMapper.selectQuestionPage(page, questionQueryRequest);
            List<QuestionBase> records = page.getRecords();
            if (CollUtil.isEmpty(records)) {
                return new Page<>(current, pageSize, 0);
            }
            // 1. 提取题目 ID，批量查询关联表
            List<Long> questionIdList = records.stream().map(QuestionBase::getId).collect(Collectors.toList());
            List<QuestionTagRelated> relatedList = questionTagRelatedService.lambdaQuery()
                    .in(QuestionTagRelated::getQuestionId, questionIdList)
                    .list();

            // 2. 提取标签 ID，批量查询标签表并转为 Map (tagId -> QuestionTag)
            Set<Long> tagIdSet = relatedList.stream().map(QuestionTagRelated::getQuestionTagId).collect(Collectors.toSet());
            // 增加判空，防止没有标签时 listByIds 报错
            Map<Long, QuestionTag> tagMap = CollUtil.isEmpty(tagIdSet) ? Collections.emptyMap() :
                    questionTagService.listByIds(tagIdSet).stream()
                    .collect(Collectors.toMap(QuestionTag::getId, tag -> tag));

            // 3. 直接将关联记录按题目 ID 分组，并且 value 只提取 tagId，不需要完整的 Related 对象
            Map<Long, List<Long>> questionIdToTagIdsMap = relatedList.stream()
                    .collect(Collectors.groupingBy(
                            QuestionTagRelated::getQuestionId,
                            Collectors.mapping(QuestionTagRelated::getQuestionTagId, Collectors.toList())
                    ));

            // 4. 组装最终的 VO 列表 (使用 map 函数替代 forEach 外挂 List)
            questionVOList = records.stream().map(question -> {
                QuestionBaseVO vo = new QuestionBaseVO();
                BeanUtil.copyProperties(question, vo);

                // getOrDefault 完美解决 NPE 问题：如果该题目没查到标签关联，默认给个空 List
                List<Long> tagIds = questionIdToTagIdsMap.getOrDefault(question.getId(), Collections.emptyList());

                // 将 tagId 转换为具体的 Tag 对象 (顺手过滤掉数据库可能存在的脏数据/无效标签)
                List<QuestionTag> tags = tagIds.stream()
                        .map(tagMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                vo.setTags(tags);
                return vo;
            }).collect(Collectors.toList());
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(questionVOList), 10 + RandomUtil.randomInt(-2, 2), TimeUnit.MINUTES);

        } else {
            questionVOList = JSONUtil.toList(cacheStr, QuestionBaseVO.class);
        }

        String key = "user:ac:" + userService.getLoginUser(request).getId();
        Set<String> passedQuestionIds = stringRedisTemplate.opsForSet().members(key);
        questionVOList.forEach(vo -> {
                    if (passedQuestionIds != null) {
                        vo.setIsAccepted(passedQuestionIds.contains(vo.getId().toString()));
                    }
                }
        );
        Page<QuestionBaseVO> voPage = new Page<>(current, pageSize, page.getTotal());
        voPage.setRecords(questionVOList);
        return voPage;

    }

    @Resource
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateQuestion(QuestionUpdateRequest questionUpdateRequest) {
        String cacheKey = "cache:questionItem:" + questionUpdateRequest.getId();
        stringRedisTemplate.delete(cacheKey);
        QuestionBase questionBase = new QuestionBase();
        BeanUtils.copyProperties(questionUpdateRequest, questionBase);
        QuestionExtent questionExtent = new QuestionExtent();
        BeanUtils.copyProperties(questionUpdateRequest, questionExtent);

        questionExtent.setSampleJudgeCase(JSONUtil.toJsonStr(questionUpdateRequest.getSampleJudgeCaseList()));
        questionExtent.setHiddenJudgeCase(JSONUtil.toJsonStr(questionUpdateRequest.getHiddenJudgeCase()));
        // 参数校验
        this.validate(questionBase, questionExtent);
        long questionId = questionUpdateRequest.getId();
        // 判断是否存在
        QuestionExtent oldQuestionExtent = questionExtentService.getById(questionId);
        ThrowUtils.throwIf(oldQuestionExtent == null, ErrorCode.NOT_FOUND_ERROR);
        HiddenJudgeCase oldJudgeCase = JSONUtil.toBean(oldQuestionExtent.getHiddenJudgeCase(), HiddenJudgeCase.class);


        //添加基本和扩展信息
        boolean b = this.updateById(questionBase);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        boolean b1 = questionExtentService.updateById(questionExtent);
        ThrowUtils.throwIf(!b1, ErrorCode.OPERATION_ERROR);
        //添加标签信息
        List<Long> relatedIds = questionTagRelatedService
                .lambdaQuery()
                .select(QuestionTagRelated::getId)
                .eq(QuestionTagRelated::getQuestionId, questionId)
                .list()
                .stream().map(QuestionTagRelated::getId)
                .collect(Collectors.toList());
        questionTagRelatedService.removeBatchByIds(relatedIds);
        List<Long> tagIds = questionUpdateRequest.getTagIds();
        if (!tagIds.isEmpty()) {
            List<QuestionTagRelated> questionTagRelatedList = tagIds.stream()
                    .map(questionTagId -> {
                        QuestionTagRelated related = new QuestionTagRelated();
                        related.setQuestionId(questionId);
                        related.setQuestionTagId(questionTagId);
                        return related;
                    }).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(questionTagRelatedList)) {
                boolean batchResult = questionTagRelatedService.saveBatch(questionTagRelatedList);
                ThrowUtils.throwIf(!batchResult, ErrorCode.OPERATION_ERROR);
            }
        }
        scheduledThreadPoolExecutor.schedule(() -> {
            stringRedisTemplate.delete(cacheKey);
        }, 500, TimeUnit.MILLISECONDS);
        if (!questionUpdateRequest.getHiddenJudgeCase().equals(oldJudgeCase)) {
            try {
                cosManager.deleteObject(oldJudgeCase.getKey());
            } catch (Exception e) {
                log.error("COS文件删除失败,{}", e.getMessage());
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteQuestion(DeleteRequest deleteRequest) {
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBase oldQuestion = this.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        boolean b = this.removeById(id);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        boolean b1 = questionExtentService.removeById(id);
        ThrowUtils.throwIf(!b1, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public void validate(QuestionBase questionBase, QuestionExtent questionExtent) {
        if (questionBase != null) {
            String title = questionBase.getTitle();
            String difficulty = questionBase.getDifficulty();
            String number = questionBase.getNumber();
            if (StrUtil.isBlank(title) || title.length() > 64) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目为空或过长");
            }
            if (StrUtil.isBlank(number) || number.length() > 64) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号为空或过长");
            }
            QuestionDifficultyEnum eum = QuestionDifficultyEnum.getEnumByValue(difficulty);
            if (eum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目难度选择异常");
            }
        }
        if (questionExtent != null) {
            String content = questionExtent.getContent();
            if (StrUtil.isBlank(content) || content.length() > 6192) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容为空或过长");
            }
        }
    }

    @Resource
    private RedisCacheUtils redisCacheUtils;

    @Override
    public void reloadCache() {
        redisCacheUtils.deleteByPattern("cache:questionPage:*");
    }


}




