package org.dhis2.composetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhis2.composetable.R
import org.dhis2.composetable.model.TextInputModel
import org.dhis2.composetable.model.toKeyboardType

@Composable
fun TextInput(
    textInputModel: TextInputModel,
    onTextChanged: (TextInputModel) -> Unit,
    onSave: (TextInputModel) -> Unit
) {
    Column(
        modifier = Modifier
            .testTag(INPUT_TEST_TAG)
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
    ) {
        InputTitle(textInputModel)
        TextInputContent(
            textInputModel,
            onTextChanged = onTextChanged,
            onSave = onSave
        )
    }
}

@Composable
private fun InputTitle(textInputModel: TextInputModel) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = displayName(
                textInputModel.mainLabel,
                textInputModel.secondaryLabels
            ),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun TextInputContent(
    textInputModel: TextInputModel,
    onTextChanged: (TextInputModel) -> Unit,
    onSave: (TextInputModel) -> Unit
) {
    var value by remember(textInputModel.currentValue) {
        mutableStateOf(
            textInputModel.currentValue ?: ""
        )
    }

    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BasicTextField(
                modifier = Modifier
                    .testTag(INPUT_TEST_FIELD_TEST_TAG)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onFocusChanged {
                        hasFocus = it.isFocused
                    },
                value = value,
                onValueChange = {
                    value = it
                    onTextChanged(textInputModel.copy(currentValue = value))
                },
                textStyle = TextStyle.Default.copy(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (textInputModel.keyboardInputType.forceCapitalize) {
                        KeyboardCapitalization.Characters
                    } else {
                        KeyboardCapitalization.None
                    },
                    imeAction = ImeAction.Next,
                    keyboardType = textInputModel.keyboardInputType.toKeyboardType()
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.clearFocus(force = true)
                        onSave(textInputModel.copy(currentValue = value))
                    }
                )
            )
            Spacer(modifier = Modifier.size(3.dp))
            Divider(
                color = if (hasFocus) {
                    LocalTableColors.current.primary
                } else {
                    LocalTableColors.current.disabledCellText
                },
                thickness = 1.dp
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        TextInputContentActionIcon(
            modifier = Modifier
                .clickable(role = Role.Button) {
                    if (hasFocus) {
                        focusManager.clearFocus(force = true)
                        onSave(textInputModel.copy(currentValue = value))
                    }
                },
            hasFocus = hasFocus
        )
    }
}

@Composable
private fun TextInputContentActionIcon(
    modifier: Modifier = Modifier,
    hasFocus: Boolean
) {
    val icon = if (hasFocus) {
        R.drawable.ic_finish_edit_input
    } else {
        R.drawable.ic_edit_input
    }

    Icon(
        modifier = modifier
            .semantics {
                drawableId = icon
            },
        painter = painterResource(id = icon),
        tint = LocalTableColors.current.primary,
        contentDescription = ""
    )
}

@Composable
fun displayName(
    dataElementName: String,
    categoryOptionComboOptionNames: List<String>
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = LocalTableColors.current.headerText
            )
        ) {
            append(dataElementName)
        }

        categoryOptionComboOptionNames.forEach { catOptionName ->
            withStyle(
                style = SpanStyle(
                    color = LocalTableColors.current.primary
                )
            ) {
                append(" / ")
            }
            withStyle(
                style = SpanStyle(
                    color = LocalTableColors.current.disabledCellText
                )
            ) {
                append(catOptionName)
            }
        }
    }
}

@Preview
@Composable
fun DefaultTextInputStatusPreview() {
    val previewTextInput = TextInputModel(
        id = "",
        mainLabel = "Row",
        secondaryLabels = listOf("header 1", "header 2"),
        currentValue = "Test"
    )
    TextInput(textInputModel = previewTextInput, onTextChanged = {}, onSave = {})
}

@Preview
@Composable
fun ActiveEditionTextInputStatusPreview() {
    val previewTextInput = TextInputModel(
        id = "",
        mainLabel = "Row",
        secondaryLabels = listOf("header 1", "header 2"),
        currentValue = "Test"
    )
    TextInput(textInputModel = previewTextInput, onTextChanged = {}, onSave = {})
}

const val INPUT_TEST_TAG = "INPUT_TEST_TAG"
const val INPUT_TEST_FIELD_TEST_TAG = "INPUT_TEST_FIELD_TEST_TAG"

val DrawableId = SemanticsPropertyKey<Int>("DrawableResId")
var SemanticsPropertyReceiver.drawableId by DrawableId
