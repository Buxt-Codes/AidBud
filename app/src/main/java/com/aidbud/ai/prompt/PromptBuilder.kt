package com.aidbud.ai.prompt

import android.content.Context
import com.aidbud.data.message.Message
import com.aidbud.data.viewmodel.repo.AidBudRepository
import com.aidbud.data.ragdata.RagData
import com.aidbud.data.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import com.aidbud.data.settings.CurrentContext
import kotlinx.coroutines.flow.firstOrNull

val MAX_QUERY_PROMPT_CHARACTERS = 24000
val MAX_ATTACHMENT_PROMPT_CHARACTERS = 12000

class PromptBuilder (
    private val context: Context,
    private val repository: AidBudRepository,
    private val settings: SettingsViewModel
) {
    private val templates: MutableMap<String, String> = mutableMapOf()

    init {
        loadTemplate("TriageFunctionPrompt", "xxx")
        loadTemplate("NonTriageFunctionPrompt", "xxx")
        loadTemplate("TriagePrompt", "xxx")
        loadTemplate("NonTriagePrompt", "xxx")
        loadTemplate("AttachmentProcessing", "xx")
    }

    private fun loadTemplate(name: String, assetFileName: String) {
        val text = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
        templates[name] = text
    }

    suspend fun buildOnlyQueryPrompt(
        query: String,
        attachmentRagId: Long,
        newAttachmentDescription: String? = null,
        newAttachmentTranscription: String? = null,
        oldAttachmentDescription: String? = null,
        oldAttachmentTranscription: String? = null,
        attachmentFunctionRemarks: String? = null,
        conversationId: Long,
        ragText: List<RagData> = emptyList(),
        ragAttachment: List<RagData> = emptyList()
    ) {
        val promptTemplate = if (settings.triageEnabled.value) {
            templates["TriagePrompt"]!!
        } else {
            templates["NonTriagePrompt"]!!
        }

        val triageSection = StringBuilder()
        val firstAidAccessSection = StringBuilder()
        val currentContextSection = StringBuilder()

        if (settings.triageEnabled.value && settings.triage.value.isNotEmpty()) {
            val triageSectionContext = """
                Here is the context for deciding on the appropriate Triaging Level for the patient.
                You should explain how you managed to select the level of Triaging in your response to the user.
            """.trimIndent()
            triageSection.appendLine(triageSectionContext)
            triageSection.appendLine("## Triage Priority Levels:")
            settings.triage.value.forEach { (level, description) ->
                triageSection.appendLine("- $level: $description")
            }
            triageSection.appendLine()
        }

        if (settings.firstAidEnabled.value) {
            val firstAidSectionContext = """
                Here is the context for deciding on the appropriate Intervention Plan for the patient given the current First Aid Availability. 
                Where IMMEDIATE is available, NON_IMMEDIATE signifies that it's not conveniently obtainable and NO ACCESS means that no First Aid is available. 
                This context should be used when deciding on the Intervention Plan, where bandages, alcohol swabs, and more are located in.
            """.trimIndent()
            triageSection.appendLine(firstAidSectionContext)
            firstAidAccessSection.appendLine("## First aid access level:")
            firstAidAccessSection.appendLine(settings.firstAidAccess.value.name)
            firstAidAccessSection.appendLine()
        }

        if (settings.contextEnabled.value) {
            val (context, customText) = settings.currentContext.value
            val contextText = if (context == CurrentContext.CUSTOM) {
                customText ?: "Custom Context"
            } else {
                context.name
            }
            val triageCurrentContextContext = """
                Here is the context for deciding on the appropriate Intervention Plan for the patient given the current context. 
                This contexts aids to inform you the current environment in which the user and patient are in. It can serve as context
                as to how the patient sustained their injuries although this should not influence your conclusions on the injury diagnosis.
            """.trimIndent()
            triageSection.appendLine(triageCurrentContextContext)
            currentContextSection.appendLine("## Current context:")
            currentContextSection.appendLine(contextText)
            currentContextSection.appendLine()
        }

        val pCard = repository.getPCardsForConversation(conversationId).firstOrNull()?.firstOrNull()
        val pCardDetails = pCard?.let {
            buildString {
                if (settings.triageEnabled.value) {
                    appendLine("Triage Level: ${if (it.triageLevel.isNullOrBlank()) "null" else it.triageLevel}")
                }
                appendLine("Injury Identification: ${if (it.injuryIdentification.isNullOrBlank()) "null" else it.injuryIdentification}")
                appendLine("Identified Injury Description: ${if (it.identifiedInjuryDescription.isNullOrBlank()) "null" else it.identifiedInjuryDescription}")
                appendLine("Patient's Injury Description: ${if (it.patientInjuryDescription.isNullOrBlank()) "null" else it.patientInjuryDescription}")
                appendLine("Intervention Plan: ${if (it.interventionPlan.isNullOrBlank()) "null" else it.interventionPlan}")
            }.trim()
        }

        val newAttachmentDescriptionSection = StringBuilder()
        val newAttachmentTranscriptionSection = StringBuilder()

        if (newAttachmentDescription != null) {
            newAttachmentDescriptionSection.appendLine("## Updated $attachmentRagId Attachment Description:")
            newAttachmentDescriptionSection.appendLine(newAttachmentDescription)
        }
        if (newAttachmentTranscription != null) {
            newAttachmentTranscriptionSection.appendLine("## Updated $attachmentRagId Attachment Transcription:")
            newAttachmentTranscriptionSection.appendLine(newAttachmentTranscription)
        }

        val oldAttachmentDescriptionSection = StringBuilder()
        val oldAttachmentTranscriptionSection = StringBuilder()

        if (oldAttachmentDescription != null) {
            oldAttachmentDescriptionSection.appendLine("## Old $attachmentRagId Attachment Description:")
            oldAttachmentDescriptionSection.appendLine(oldAttachmentDescription)
        }
        if (oldAttachmentTranscription != null) {
            oldAttachmentTranscriptionSection.appendLine("## Old $attachmentRagId Attachment Transcription:")
            oldAttachmentTranscriptionSection.appendLine(oldAttachmentTranscription)
        }

        val attachmentFunctionRemarksSection = StringBuilder()
        if (attachmentFunctionRemarks != null) {
            attachmentFunctionRemarksSection.appendLine("## $attachmentRagId Attachment Function Remarks:")
            attachmentFunctionRemarksSection.appendLine(attachmentFunctionRemarks)
        }

        val initialPrompt = promptTemplate
            .replace("[PCARD DETAILS]", pCardDetails ?: "")
            .replace("[TRIAGE SECTION]", triageSection.toString())
            .replace("[FIRST AID ACCESS SECTION]", firstAidAccessSection.toString())
            .replace("[CURRENT CONTEXT SECTION]", currentContextSection.toString())
            .replace("[USER QUERY]", query)
            .replace("[NEW ATTACHMENT DESCRIPTION SECTION]", newAttachmentDescriptionSection.toString())
            .replace("[NEW ATTACHMENT TRANSCRIPTION SECTION]", newAttachmentTranscriptionSection.toString())
            .replace("[OLD ATTACHMENT DESCRIPTION SECTION]", oldAttachmentDescriptionSection.toString())
            .replace("[OLD ATTACHMENT TRANSCRIPTION SECTION]", oldAttachmentTranscriptionSection.toString())

        val characterCountLeft = MAX_QUERY_PROMPT_CHARACTERS - initialPrompt.length - 1000

        val allocMessages = characterCountLeft * 2 / 5
        val allocRagText = characterCountLeft * 2 / 5
        val allocAttachments = characterCountLeft * 1 / 5

        val latestMessages = repository.getMessagesForConversationLimit(conversationId, 6)
            .first()
            .asReversed()

        fun <T> accumulate(
            items: List<T>,
            charLimit: Int,
            formatter: (T) -> String
        ): String {
            val sb = StringBuilder()
            var used = 0
            for (item in items) {
                val entry = formatter(item)
                if (used + entry.length > charLimit) break
                sb.append(entry)
                used += entry.length
            }
            return sb.toString()
        }

        val msgFormatter: (Message) -> String = { m ->
            buildString {
                append("Role: ${m.role}\n")
                append("Text: ${m.text}\n")
                if (m.attachments != null) {
                    if (m.attachments.isNotEmpty()) {
                        append("attachments: true\n")
                    }
                }
                if (!m.pCardChanges.isNullOrBlank()) {
                    append("PCardChanges: ${m.pCardChanges}\n")
                }
                append("\n")
            }
        }

        val ragTextFormatter: (RagData) -> String = { r ->
            buildString {
                val query = r.data["query"] as? String
                val response = r.data["response"] as? String
                val pcard = r.data["pCardChanges"] as? String

                if (!query.isNullOrBlank()) append("Query: $query\n")
                if (!response.isNullOrBlank()) append("Response: $response\n")
                if (!pcard.isNullOrBlank()) append("PCardChanges: $pcard\n")

                append("\n")
            }
        }

        val ragAttachFormatter: (RagData) -> String = { r ->
            buildString {
                append("AttachmentId: ${r.ragDataId}\n")

                val description = r.data["description"] as? String
                val transcription = r.data["transcription"] as? String

                if (!description.isNullOrBlank()) append("Description: $description\n")
                if (!transcription.isNullOrBlank()) append("Transcription: $transcription\n")

                append("\n")
            }
        }

        val latestMessagesBody = accumulate(latestMessages, allocMessages, msgFormatter)
        val ragTextBody = accumulate(ragText, allocRagText, ragTextFormatter)
        val ragAttachmentBody = accumulate(ragAttachment, allocAttachments, ragAttachFormatter)

        val conversationSection =
            if (latestMessagesBody.isNotBlank()) "## Latest Conversation:\n$latestMessagesBody" else ""
        val ragTextSection = if (ragTextBody.isNotBlank()) "## RAG Text:\n$ragTextBody" else ""
        val ragAttachmentSection =
            if (ragAttachmentBody.isNotBlank()) " ## RAG Attachments:\n$ragAttachmentBody" else ""

        val prompt = initialPrompt
            .replace("[CONVERSATION HISTORY]", conversationSection)
            .replace("[RAG TEXT]", ragTextSection)
            .replace("[RAG ATTACHMENTS]", ragAttachmentSection)

        return prompt
    }


    suspend fun buildQueryWithFunctionPrompt(
        query: String,
        attachmentDescription: String? = null,
        attachmentTranscription: String? = null,
        conversationId: Long,
        ragText: List<RagData> = emptyList(),
        ragAttachment: List<RagData> = emptyList()
    ): String {
        val promptTemplate = if (settings.triageEnabled.value) {
            templates["TriageFunctionPrompt"]!!
        } else {
            templates["NonTriageFunctionPrompt"]!!
        }

        val triageSection = StringBuilder()
        val firstAidAccessSection = StringBuilder()
        val currentContextSection = StringBuilder()

        if (settings.triageEnabled.value && settings.triage.value.isNotEmpty()) {
            val triageSectionContext = """
                Here is the context for deciding on the appropriate Triaging Level for the patient.
                You should explain how you managed to select the level of Triaging in your response to the user.
            """.trimIndent()
            triageSection.appendLine(triageSectionContext)
            triageSection.appendLine("## Triage Priority Levels:")
            settings.triage.value.forEach { (level, description) ->
                triageSection.appendLine("- $level: $description")
            }
            triageSection.appendLine()
        }

        if (settings.firstAidEnabled.value) {
            val firstAidSectionContext = """
                Here is the context for deciding on the appropriate Intervention Plan for the patient given the current First Aid Availability. 
                Where IMMEDIATE is available, NON_IMMEDIATE signifies that it's not conveniently obtainable and NO ACCESS means that no First Aid is available. 
                This context should be used when deciding on the Intervention Plan, where bandages, alcohol swabs, and more are located in.
            """.trimIndent()
            triageSection.appendLine(firstAidSectionContext)
            firstAidAccessSection.appendLine("## First aid access level:")
            firstAidAccessSection.appendLine(settings.firstAidAccess.value.name)
            firstAidAccessSection.appendLine()
        }

        if (settings.contextEnabled.value) {
            val (context, customText) = settings.currentContext.value
            val contextText = if (context == CurrentContext.CUSTOM) {
                customText ?: "Custom Context"
            } else {
                context.name
            }
            val triageCurrentContextContext = """
                Here is the context for deciding on the appropriate Intervention Plan for the patient given the current context. 
                This contexts aids to inform you the current environment in which the user and patient are in. It can serve as context
                as to how the patient sustained their injuries although this should not influence your conclusions on the injury diagnosis.
            """.trimIndent()
            triageSection.appendLine(triageCurrentContextContext)
            currentContextSection.appendLine("## Current context:")
            currentContextSection.appendLine(contextText)
            currentContextSection.appendLine()
        }

        val pCard = repository.getPCardsForConversation(conversationId).firstOrNull()?.firstOrNull()
        val pCardDetails = pCard?.let {
            buildString {
                if (settings.triageEnabled.value) {
                    appendLine("Triage Level: ${if (it.triageLevel.isNullOrBlank()) "null" else it.triageLevel}")
                }
                appendLine("Injury Identification: ${if (it.injuryIdentification.isNullOrBlank()) "null" else it.injuryIdentification}")
                appendLine("Identified Injury Description: ${if (it.identifiedInjuryDescription.isNullOrBlank()) "null" else it.identifiedInjuryDescription}")
                appendLine("Patient's Injury Description: ${if (it.patientInjuryDescription.isNullOrBlank()) "null" else it.patientInjuryDescription}")
                appendLine("Intervention Plan: ${if (it.interventionPlan.isNullOrBlank()) "null" else it.interventionPlan}")
            }.trim()
        }

        val attachmentDescriptionSection = StringBuilder()
        val attachmentTranscriptionSection = StringBuilder()

        if (attachmentDescription != null) {
            attachmentDescriptionSection.appendLine("## Query Attachment Description:")
            attachmentDescriptionSection.appendLine(attachmentDescription)
        }
        if (attachmentTranscription != null) {
            attachmentTranscriptionSection.appendLine("## Query Attachment Transcription:")
            attachmentTranscriptionSection.appendLine(attachmentTranscription)
        }

        val initialPrompt = promptTemplate
            .replace("[PCARD DETAILS]", pCardDetails ?: "")
            .replace("[TRIAGE SECTION]", triageSection.toString())
            .replace("[FIRST AID ACCESS SECTION]", firstAidAccessSection.toString())
            .replace("[CURRENT CONTEXT SECTION]", currentContextSection.toString())
            .replace("[USER QUERY]", query)
            .replace("[ATTACHMENT DESCRIPTION SECTION]", attachmentDescriptionSection.toString())
            .replace("[ATTACHMENT TRANSCRIPTION SECTION]", attachmentTranscriptionSection.toString())

        val characterCountLeft = MAX_QUERY_PROMPT_CHARACTERS - initialPrompt.length - 1000

        val allocMessages = characterCountLeft * 2 / 5
        val allocRagText = characterCountLeft * 2 / 5
        val allocAttachments = characterCountLeft * 1 / 5

        val latestMessages = repository.getMessagesForConversationLimit(conversationId, 6)
            .first()
            .asReversed()

        fun <T> accumulate(
            items: List<T>,
            charLimit: Int,
            formatter: (T) -> String
        ): String {
            val sb = StringBuilder()
            var used = 0
            for (item in items) {
                val entry = formatter(item)
                if (used + entry.length > charLimit) break
                sb.append(entry)
                used += entry.length
            }
            return sb.toString()
        }

        val msgFormatter: (Message) -> String = { m ->
            buildString {
                append("Role: ${m.role}\n")
                append("Text: ${m.text}\n")
                if (m.attachments != null) {
                    if (m.attachments.isNotEmpty()) {
                        append("attachments: true\n")
                    }
                }
                if (!m.pCardChanges.isNullOrBlank()) {
                    append("PCardChanges: ${m.pCardChanges}\n")
                }
                append("\n")
            }
        }

        val ragTextFormatter: (RagData) -> String = { r ->
            buildString {
                val query = r.data["query"] as? String
                val response = r.data["response"] as? String
                val pcard = r.data["pCardChanges"] as? String

                if (!query.isNullOrBlank()) append("Query: $query\n")
                if (!response.isNullOrBlank()) append("Response: $response\n")
                if (!pcard.isNullOrBlank()) append("PCardChanges: $pcard\n")

                append("\n")
            }
        }

        val ragAttachFormatter: (RagData) -> String = { r ->
            buildString {
                append("AttachmentId: ${r.ragDataId}\n")

                val description = r.data["description"] as? String
                val transcription = r.data["transcription"] as? String

                if (!description.isNullOrBlank()) append("Description: $description\n")
                if (!transcription.isNullOrBlank()) append("Transcription: $transcription\n")

                append("\n")
            }
        }

        val latestMessagesBody = accumulate(latestMessages, allocMessages, msgFormatter)
        val ragTextBody = accumulate(ragText, allocRagText, ragTextFormatter)
        val ragAttachmentBody = accumulate(ragAttachment, allocAttachments, ragAttachFormatter)

        val conversationSection =
            if (latestMessagesBody.isNotBlank()) "## Latest Conversation:\n$latestMessagesBody" else ""
        val ragTextSection = if (ragTextBody.isNotBlank()) "## RAG Text:\n$ragTextBody" else ""
        val ragAttachmentSection =
            if (ragAttachmentBody.isNotBlank()) " ## RAG Attachments:\n$ragAttachmentBody" else ""

        val prompt = initialPrompt
            .replace("[CONVERSATION HISTORY]", conversationSection)
            .replace("[RAG TEXT]", ragTextSection)
            .replace("[RAG ATTACHMENTS]", ragAttachmentSection)

        return prompt
    }

    suspend fun buildAttachmentPrompt(
        query: String,
        additionalComment: String? = null,
        transcription: String? = null,
        ragDataId: Long? = null
    ): String {
        val promptTemplate = templates["AttachmentProcessing"]!!
        val additionalCommentSection =
            if (additionalComment?.isNotBlank() ?: false) "## Additional Comments:\n$additionalComment" else ""
        val transcriptionSection =
            if (transcription?.isNotBlank() ?: false) "## Additional Comments:\n$transcription" else ""

        val prompt = promptTemplate
            .replace("[QUERY]", query)
            .replace("[ADDITIONAL COMMENTS]", additionalCommentSection)
            .replace("[ATTACHMENT TRANSCRIPTION]", transcriptionSection)

        if (ragDataId != null) {
            val attachment = repository.getRagDataById(ragDataId).first()!!
            val attachmentDescription = attachment.data.get("description") as? String ?: ""
            val attachmentDescriptionSection =
                if (attachmentDescription.isNotBlank()) "## Description:\n$attachmentDescription" else ""
            val finalPrompt = prompt
                .replace("[ATTACHMENT DESCRIPTION]", attachmentDescriptionSection)
            return finalPrompt
        } else {
            val finalPrompt = prompt
                .replace("[ATTACHMENT TRANSCRIPTION]", "")
                .replace("[ATTACHMENT DESCRIPTION]", "")
            return finalPrompt
        }
    }
}