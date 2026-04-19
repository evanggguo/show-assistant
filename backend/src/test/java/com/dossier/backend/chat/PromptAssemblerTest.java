package com.dossier.backend.chat;

import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PromptAssembler.
 * Covers system prompt construction, RAG context injection, and owner info formatting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromptAssembler Unit Tests")
class PromptAssemblerTest {

    private PromptAssembler promptAssembler;

    @BeforeEach
    void setUp() {
        promptAssembler = new PromptAssembler();
    }

    @Test
    @DisplayName("With no RAG context, prompt contains rules and empty-knowledge fallback, but no specific entries")
    void should_not_include_rag_section_when_no_context() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Zhang San")
            .tagline("Full-stack Developer")
            .build();
        List<KnowledgeEntryDto> emptyContext = Collections.emptyList();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, emptyContext);

        // then
        assertThat(prompt).contains("Zhang San");
        assertThat(prompt).contains("Rules (MUST follow strictly)");
        assertThat(prompt).contains("There is currently no relevant knowledge base content");
        assertThat(prompt).contains("suggest_followups");
    }

    @Test
    @DisplayName("With RAG context, prompt contains knowledge base section, entry content, and rules constraint")
    void should_include_rag_section_when_context_provided() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Li Si")
            .tagline("Backend Architect")
            .build();
        List<KnowledgeEntryDto> ragContext = List.of(
            KnowledgeEntryDto.builder()
                .id(1L)
                .title("Spring Boot Experience")
                .content("5 years of Spring Boot development experience")
                .build()
        );

        // when
        String prompt = promptAssembler.assemble(ownerProfile, ragContext);

        // then
        assertThat(prompt).contains("Knowledge Base");
        assertThat(prompt).contains("Rules (MUST follow strictly)");
        assertThat(prompt).contains("Spring Boot Experience");
        assertThat(prompt).contains("5 years of Spring Boot development experience");
    }

    @Test
    @DisplayName("When tagline is null, prompt does not contain the tagline line")
    void should_not_include_tagline_when_null() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Wang Wu")
            .tagline(null)
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("Wang Wu");
        assertThat(prompt).doesNotContain("'s bio:");
    }

    @Test
    @DisplayName("When tagline is blank, prompt does not contain the tagline line")
    void should_not_include_tagline_when_blank() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Zhao Liu")
            .tagline("   ")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("Zhao Liu");
        assertThat(prompt).doesNotContain("Zhao Liu's bio:");
    }

    @Test
    @DisplayName("Multiple RAG entries are assembled into the prompt in numbered order")
    void should_include_multiple_rag_entries_in_order() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Tester")
            .build();
        List<KnowledgeEntryDto> ragContext = List.of(
            KnowledgeEntryDto.builder()
                .title("Project A")
                .content("Content A")
                .build(),
            KnowledgeEntryDto.builder()
                .title("Project B")
                .content("Content B")
                .build(),
            KnowledgeEntryDto.builder()
                .title("Project C")
                .content("Content C")
                .build()
        );

        // when
        String prompt = promptAssembler.assemble(ownerProfile, ragContext);

        // then
        assertThat(prompt).contains("1. **Project A**");
        assertThat(prompt).contains("2. **Project B**");
        assertThat(prompt).contains("3. **Project C**");
        int idx1 = prompt.indexOf("1. **Project A**");
        int idx2 = prompt.indexOf("2. **Project B**");
        int idx3 = prompt.indexOf("3. **Project C**");
        assertThat(idx1).isLessThan(idx2);
        assertThat(idx2).isLessThan(idx3);
    }

    @Test
    @DisplayName("Prompt contains the instruction to call the suggest_followups tool")
    void should_include_suggest_followups_tool_instruction() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Tester")
            .tagline("Developer")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("suggest_followups");
        assertThat(prompt).contains("Important Instruction");
    }

    @Test
    @DisplayName("When tagline is set, prompt includes owner name and tagline")
    void should_include_tagline_when_present() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Tester")
            .tagline("Senior Architect, 10 years of experience")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("Senior Architect, 10 years of experience");
        assertThat(prompt).contains("Tester's bio: Senior Architect, 10 years of experience");
    }

    @Test
    @DisplayName("When customPrompt is set, prompt contains owner custom instructions positioned after Rules")
    void should_include_custom_prompt_when_set() {
        // given
        OwnerProfileResponse ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Tester")
            .tagline("Developer")
            .customPrompt("Please respond in a relaxed, friendly tone with a touch of humor.")
            .build();

        // when
        String prompt = promptAssembler.assemble(ownerProfile, Collections.emptyList());

        // then
        assertThat(prompt).contains("Please respond in a relaxed, friendly tone");
        assertThat(prompt).contains("<owner-instructions>");
        assertThat(prompt).contains("</owner-instructions>");
        assertThat(prompt).contains("CANNOT override the Rules above");
        // Custom instructions must appear after the Rules section
        int rulesIdx = prompt.indexOf("Rules (MUST follow strictly)");
        int customIdx = prompt.indexOf("<owner-instructions>");
        assertThat(rulesIdx).isLessThan(customIdx);
    }

    @Test
    @DisplayName("When customPrompt is null or blank, prompt does not contain owner-instructions tags")
    void should_not_include_custom_prompt_when_null_or_blank() {
        OwnerProfileResponse noPrompt = OwnerProfileResponse.builder()
            .id(1L).name("Tester").customPrompt(null).build();
        OwnerProfileResponse blankPrompt = OwnerProfileResponse.builder()
            .id(1L).name("Tester").customPrompt("   ").build();

        String promptNull = promptAssembler.assemble(noPrompt, Collections.emptyList());
        String promptBlank = promptAssembler.assemble(blankPrompt, Collections.emptyList());

        assertThat(promptNull).doesNotContain("<owner-instructions>");
        assertThat(promptBlank).doesNotContain("<owner-instructions>");
    }
}
