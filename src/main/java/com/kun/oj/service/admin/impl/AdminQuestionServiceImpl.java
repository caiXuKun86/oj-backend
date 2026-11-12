package com.kun.oj.service.admin.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.aliyun.oss.model.PutObjectResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.ResultCode;
import com.kun.oj.constant.RedisConstants;
import com.kun.oj.enums.QuestionDifficultyEnum;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.manage.oss.OssTemplate;
import com.kun.oj.mapper.QuestionMapper;
import com.kun.oj.model.dto.request.AdminQuestionPageRequest;
import com.kun.oj.model.dto.request.QuestionAddRequest;
import com.kun.oj.model.dto.request.QuestionUpdateRequest;
import com.kun.oj.model.dto.response.JudgeCaseUploadResponse;
import com.kun.oj.model.entity.Question;
import com.kun.oj.model.vo.QuestionPageVO;
import com.kun.oj.model.vo.QuestionVO;
import com.kun.oj.service.admin.AdminQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Lenovo
 * @description 针对表【question(题目)】的数据库操作Service实现
 * @createDate 2026-10-31 10:44:23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements AdminQuestionService {

    @Value("${oj.path.judge-case-upload}")
    private String judgeCaseUploadPath;
    private final OssTemplate ossTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<QuestionPageVO> queryQuestionPage(AdminQuestionPageRequest pageRequest) {

        Long id = pageRequest.getId();
        String number = pageRequest.getNumber();
        String title = pageRequest.getTitle();
        String tag = pageRequest.getTag();
        String difficulty = pageRequest.getDifficulty();


        if (StrUtil.isNotBlank(difficulty) && QuestionDifficultyEnum.getByCode(difficulty) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "难度类型不存在");
        }
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<Question>()
                .eq(id != null, Question::getId, id)
                .eq(StrUtil.isNotBlank(difficulty), Question::getDifficulty, difficulty)
                .like(StrUtil.isNotBlank(number), Question::getNumber, number)
                .like(StrUtil.isNotBlank(title), Question::getTitle, title)
                .like(StrUtil.isNotBlank(tag), Question::getTags, tag);


        if (StrUtil.isBlank(pageRequest.getSortField())) {
            queryWrapper.orderByAsc(true, Question::getCreateTime);
        }

        Page<Question> questionPage = this.page(pageRequest.toPage(), queryWrapper);
        return PageResult.of(questionPage, question -> BeanUtil.copyProperties(question, QuestionPageVO.class));
    }

    @Override
    public QuestionVO queryQuestionDetail(Long questionId) {
        Question question = this.getById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
        }
        return BeanUtil.copyProperties(question, QuestionVO.class);
    }

    @Override
    public Long addQuestion(QuestionAddRequest questionAddRequest) {
        Question question = BeanUtil.copyProperties(questionAddRequest, Question.class);

        boolean save = this.save(question);
        if (!save) {
            throw new BusinessException(ResultCode.OPERATION_ERROR);
        }
        return question.getId();
    }

    @Override
    public JudgeCaseUploadResponse uploadTestCases(MultipartFile multipartFile, Long questionId) {
        Question oldQuestion = this.getById(questionId);
        if (oldQuestion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
        }

        // 定义全局基础路径
        String globalPath = judgeCaseUploadPath + File.separator + questionId;

        // 0.检查文件类型 (注意：Hutool 提取的后缀不带点，且最好忽略大小写)
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (!"zip".equalsIgnoreCase(suffix)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "只支持 zip 文件");
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
                throw new BusinessException(ResultCode.OPERATION_ERROR, "上传失败");
            }

            // 解压文件
            ZipUtil.unzip(zipFile, unzipDir, StandardCharsets.UTF_8);

            // 2. 检查用例是否合规，统计用例数量
            List<File> files = FileUtil.loopFiles(unzipDir);
            if (CollUtil.isEmpty(files) || files.size() % 2 != 0) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "测试用例为空或未成对");
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
                    throw new BusinessException(ResultCode.PARAMS_ERROR, "测试用例包含非法文件: " + fileName);
                }

                // 严格检查成对情况：只要是 in 文件，必须有对应的 out 文件
                if ("in".equalsIgnoreCase(caseSuffix)) {
                    if (!fileNameSet.contains(mainName + ".out")) {
                        throw new BusinessException(ResultCode.PARAMS_ERROR, "测试用例缺失输出文件: " + mainName + ".out");
                    }
                }
            }

            // 3. 上传到 OSS
            String key = "judgeCase/" + questionId + "/" + zipFileName;
            PutObjectResult putObjectResult = ossTemplate.uploadFile(key, zipFile);

            int caseCount = files.size() / 2;
            String version = putObjectResult.getETag();

            // 4. 直接更新题目数据库记录
            Question updateQuestion = new Question();
            updateQuestion.setId(questionId);
            updateQuestion.setJudgeCaseKey(key);
            updateQuestion.setJudgeCaseCount(caseCount);
            updateQuestion.setJudgeCaseVersion(version);

            boolean updated = this.updateById(updateQuestion);
            if (!updated) {
                throw new BusinessException(ResultCode.OPERATION_ERROR, "更新题目用例到数据库失败");
            }

            // 5. 若此前有旧的测试用例文件且 key 不同，清理 OSS 上的旧文件
            String oldKey = oldQuestion.getJudgeCaseKey();
            if (StrUtil.isNotBlank(oldKey) && !oldKey.equals(key)) {
                try {
                    ossTemplate.deleteFile(oldKey);
                } catch (Exception e) {
                    log.error("旧的测试用例删除失败: oldKey={}, err={}", oldKey, e.getMessage());
                }
            }

            // 6. 清理 Redis 缓存
            String redisKey = String.format(RedisConstants.CACHE_QUESTION_INFO, questionId);
            stringRedisTemplate.delete(redisKey);

            // 返回 OSS Key、用例组数和版本号
            return new JudgeCaseUploadResponse(key, caseCount, version);

        } catch (BusinessException e) {
            // BusinessException 直接往外抛，保留原有的错误信息提示给前端
            throw e;
        } catch (Exception e) {
            log.error("处理测试用例失败, {}", e.getMessage());
            throw new BusinessException(ResultCode.OPERATION_ERROR, "处理失败");

        } finally {
            // 7. 清理临时文件 (兜底策略，放在 finally 中确保一定执行)
            FileUtil.del(globalPath);
        }
    }

    @Override
    public boolean updateQuestion(QuestionUpdateRequest questionUpdateRequest, Long questionId) {
        Question oldQuestion = this.getById(questionId);
        if (oldQuestion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
        }

        Question question = BeanUtil.copyProperties(questionUpdateRequest, Question.class);
        question.setId(questionId);

        // 如果更新请求未传 judgeCase 字段，则保留原题目的测试用例信息，防止被覆盖为空
        String judgeCaseKey = questionUpdateRequest.getJudgeCaseKey();
        if (StrUtil.isBlank(judgeCaseKey)) {
            question.setJudgeCaseKey(oldQuestion.getJudgeCaseKey());
            question.setJudgeCaseCount(oldQuestion.getJudgeCaseCount());
            question.setJudgeCaseVersion(oldQuestion.getJudgeCaseVersion());
        }

        boolean b = this.updateById(question);
        if (!b) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "更新题目失败");
        }

        // 仅在显式传入新的 judgeCaseKey 且与旧的 key 不同时，才清理 OSS 上的旧文件
        if (StrUtil.isNotBlank(judgeCaseKey) && StrUtil.isNotBlank(oldQuestion.getJudgeCaseKey())
                && !judgeCaseKey.equals(oldQuestion.getJudgeCaseKey())) {
            try {
                ossTemplate.deleteFile(oldQuestion.getJudgeCaseKey());
            } catch (Exception e) {
                log.error("旧的测试用例删除失败: {}", e.getMessage());
            }
        }

        String redisKey = String.format(RedisConstants.CACHE_QUESTION_INFO, questionId);
        stringRedisTemplate.delete(redisKey);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteQuestion(Long questionId) {
        Question oldQuestion = this.getById(questionId);
        if (oldQuestion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
        }
        boolean b = this.removeById(questionId);
        if (!b) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "删除题目失败");
        }
        if (StrUtil.isNotBlank(oldQuestion.getJudgeCaseKey())) {
            try {
                ossTemplate.deleteFile(oldQuestion.getJudgeCaseKey());
            } catch (Exception e) {
                log.error("旧的测试用例删除失败: {}", e.getMessage());
            }
        }
        String redisKey = String.format(RedisConstants.CACHE_QUESTION_INFO, questionId);
        stringRedisTemplate.delete(redisKey);
        return true;
    }

}




