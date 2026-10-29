package com.cmyhyq.ai.agent;

import com.cmyhyq.ai.prompt.ProfilePrompt;
import com.cmyhyq.pojo.entity.UserTag;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileAgent {

    private final ChatClient chatClient;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * 更新用户画像逻辑（内部核心）
     */
    private List<UserTag> updateProfileInternal(Long userId, String question, String chatContext) {
        try {
            User user = userService.getById(userId);

//            //昵称先这样临时生成一下
//            if (user == null) {
//                user = new User();
//                user.setUserId(userId);
//                user.setNickname("游客" + userId);
//                userService.save(user);
//            }

            // 1. 读取旧标签（JSON -> List<UserTag>）
            List<UserTag> oldTags = new ArrayList<>();
            if (user.getTagsJson() != null && !user.getTagsJson().isBlank()) {
                //把 JSON字符串 → 转成 List<UserTag>
                oldTags = objectMapper.readValue(user.getTagsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, UserTag.class));
            }

            // 2. 时间衰减处理
            applyTimeDecay(oldTags);

            // 3. 提取新标签（只传名字给 LLM）
            String newTagsJson = chatClient.prompt()
                    .user(u -> u.text(ProfilePrompt.PROFILE_EXTRACTION_PROMPT)
                            .param("user_question", question)
                            .param("chat_context", chatContext))
                    .call()
                    .content();

            List<String> extractedTags = objectMapper.readValue(newTagsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            // 4. 合并新标签并更新权重
            mergeTags(oldTags, extractedTags);

            // 5. 排序并截断（保留权重最高的3个）
            List<UserTag> finalTags = filterTopTags(oldTags);

            // 6. 保存回数据库
            user.setTagsJson(objectMapper.writeValueAsString(finalTags));
            userService.updateById(user);

            log.info("用户画像更新完成, userId: {}, 标签: {}", userId, finalTags);
            return finalTags;

        } catch (Exception e) {
            log.error("ProfileAgent 更新画像失败, userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 对用户标签进行时间衰减处理
     * 根据标签的【最后更新时间】距离当前时间的天数，自动降低标签权重
     * 规则：每过去1天，权重乘以 0.95，时间越久权重越低
     * @param tags
     */
    private void applyTimeDecay(List<UserTag> tags) {
        long now = System.currentTimeMillis();
        for (UserTag tag : tags) {
            long days = (now - tag.getLastUpdateTime()) / (1000 * 60 * 60 * 24);
            if (days > 0) {
                double decay = Math.pow(0.9, days); // 每天衰减 10%
                tag.setWeight(tag.getWeight() * decay);
            }
        }
    }

    /**
     * 合并新标签：存在的权重+1，不存在的新增
     */
    private void mergeTags(List<UserTag> oldTags, List<String> newTags) {
        long now = System.currentTimeMillis();
        for (String newTagName : newTags) {
            Optional<UserTag> existing = oldTags.stream()
                    .filter(t -> t.getTag().equals(newTagName))
                    .findFirst();

            if (existing.isPresent()) {
                UserTag tag = existing.get();
                tag.setWeight(tag.getWeight() + 0.5);
                tag.setLastUpdateTime(now);
            } else {
                //给新标签打高权重，防止用户最新需求被刷下去
                UserTag tag = new UserTag(newTagName, 10, now);
                oldTags.add(tag);
            }
        }
    }

    /**
     * 过滤出权重最高的 Top 3
     */
    private List<UserTag> filterTopTags(List<UserTag> tags) {
        return tags.stream()
                .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
                .limit(3)
                .collect(Collectors.toList());
    }

    @Async
    public void updateProfileAsync(Long userId, String question, String chatContext) {
        updateProfileInternal(userId, question, chatContext);
    }

    /**
     * 同步更新并获取画像（供 RouteAgent 使用，返回给 LLM 的仍是纯标签列表）
     */
    public String updateAndGetProfile(Long userId, String question, String chatContext) {
        List<UserTag> tags = updateProfileInternal(userId, question, chatContext);
        List<String> tagNames = tags.stream().map(UserTag::getTag).collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(tagNames);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 获取当前画像的 UserTag 对象列表（供 RouteAgent 内部打分使用）
     */
    public List<UserTag> getUserTags(Long userId) {
        try {
            User user = userService.getById(userId);
            if (user != null && user.getTagsJson() != null) {
                return objectMapper.readValue(user.getTagsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, UserTag.class));
            }
        } catch (Exception e) {
            log.error("获取 UserTags 失败", e);
        }
        return new ArrayList<>();
    }
}
