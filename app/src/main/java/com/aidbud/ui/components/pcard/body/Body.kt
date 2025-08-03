package com.aidbud.ui.components.pcard.body

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import com.aidbud.data.pcard.PCard
import com.aidbud.data.viewmodel.MainViewModel

@Composable
fun SegmentList(
    pCard: PCard,
    viewModel: MainViewModel
) {

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Segment(
                title = "Triage Level",
                body = pCard.triageLevel ?: "",
                onBodyChange = {
                    viewModel.updatePCard(pCard.copy(triageLevel = it))
                }
            )

            Segment(
                title = "Injury Identification",
                body = pCard.injuryIdentification ?: "",
                onBodyChange = {
                    viewModel.updatePCard(pCard.copy(injuryIdentification = it))
                }
            )

            Segment(
                title = "Identified Injury Description",
                body = pCard.identifiedInjuryDescription ?: "",
                onBodyChange = {
                    viewModel.updatePCard(pCard.copy(identifiedInjuryDescription = it))
                }
            )

            Segment(
                title = "Patient Injury Description",
                body = pCard.patientInjuryDescription ?: "",
                onBodyChange = {
                    viewModel.updatePCard(pCard.copy(patientInjuryDescription = it))
                }
            )

            Segment(
                title = "Intervention Plan",
                body = pCard.interventionPlan ?: "",
                onBodyChange = {
                    viewModel.updatePCard(pCard.copy(interventionPlan = it))
                }
            )
        }
    }
}