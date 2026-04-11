package com.showassistant.backend.chat;

import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PromptAssembler 单元测试
 * 覆盖 System Prompt 的构建逻辑，包括 RAG 上下文注入和 Owner 信息格式化
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromptAssembler 单元测试")
class PromptAssemblerTest {

    private PromptAssembler promptAssembler;

    @BeforeEach
    void setUp() {
        promptAssembler = new PromptAssembler();
    }

    @Test
    @DisplayName("无 RAG 上下文时，prompt 不含[参考资料]段落，但包含 owner 名字和工具调用指令")
    void should_not_include_rag_section_when_no_context() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("张三")
            .tagline("全栈开发者")
            .build();
        List<KnowledgeEntryDto> emptyContext = Collections.emptyList();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, emptyContext);

        // then
        assertThat(prompt).contains("张三");
        assertThat(prompt).doesNotContain("参考资料");
        assertThat(prompt).contains("suggest_followups");
    }

    @Test
    @DisplayName("有 RAG 上下文时，prompt 包含[参考资料]段落和条目内容")
    void should_include_rag_section_when_context_provided() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("李四")
            .tagline("后端架构师")
            .build();
        List<KnowledgeEntryDto> ragContext = List.of(
            KnowledgeEntryDto.builder()
                .id(1L)
                .title("Spring Boot 经验")
                .content("5年 Spring Boot 开发经验")
                .build()
        );

        // when
        String prompt = promptAssembler.assemble(ownerProfile, ragContext);

        // then
        assertThat(prompt).contains("参考资料");
        assertThat(prompt).contains("Spring Boot 经验");
        assertThat(prompt).contains("5年 Spring Boot 开发经验");
    }

    @Test
    @DisplayName("tagline 为 null 时，prompt 中不含 tagline 行")
    void should_not_include_tagline_when_null() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("王五")
            .tagline(null)
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("王五");
        assertThat(prompt).doesNotContain("的简介：");
    }

    @Test
    @DisplayName("tagline 为空字符串时，prompt 中不含 tagline 行")
    void should_not_include_tagline_when_blank() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("赵六")
            .tagline("   ")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("赵六");
        // 空白 tagline 不应被插入
        assertThat(prompt).doesNotContain("赵六 的简介：");
    }

    @Test
    @DisplayName("多条 RAG 条目时，按编号顺序拼入 prompt")
    void should_include_multiple_rag_entries_in_order() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("测试人")
            .build();
        List<KnowledgeEntryDto> ragContext = List.of(
            KnowledgeEntryDto.builder()
                .title("项目 A")
                .content("内容 A")
                .build(),
            KnowledgeEntryDto.builder()
                .title("项目 B")
                .content("内容 B")
                .build(),
            KnowledgeEntryDto.builder()
                .title("项目 C")
                .content("内容 C")
                .build()
        );

        // when
        String prompt = promptAssembler.assemble(ownerProfile, ragContext);

        // then
        assertThat(prompt).contains("1. **项目 A**");
        assertThat(prompt).contains("2. **项目 B**");
        assertThat(prompt).contains("3. **项目 C**");
        // 验证顺序：1 出现在 2 前，2 出现在 3 前
        int idx1 = prompt.indexOf("1. **项目 A**");
        int idx2 = prompt.indexOf("2. **项目 B**");
        int idx3 = prompt.indexOf("3. **项目 C**");
        assertThat(idx1).isLessThan(idx2);
        assertThat(idx2).isLessThan(idx3);
    }

    @Test
    @DisplayName("prompt 包含要求调用 suggest_followups 工具的指令")
    void should_include_suggest_followups_tool_instruction() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("测试人")
            .tagline("开发者")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("suggest_followups");
        assertThat(prompt).contains("重要指令");
    }

    @Test
    @DisplayName("tagline 有值时，prompt 包含 owner 名字和 tagline")
    void should_include_tagline_when_present() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("测试人")
            .tagline("资深架构师，10年经验")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("资深架构师，10年经验");
        assertThat(prompt).contains("测试人 的简介：资深架构师，10年经验");
    }
}
