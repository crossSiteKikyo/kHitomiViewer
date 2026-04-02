package com.example.khitomiviewer.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = LocalTextStyle.current,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  prefix: @Composable (() -> Unit)? = null,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null,
  isError: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  minLines: Int = 1,
  interactionSource: MutableInteractionSource? = null,
  shape: Shape = OutlinedTextFieldDefaults.shape,
  colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
  contentPadding: PaddingValues = PaddingValues(2.dp)
) {
  @Suppress("NAME_SHADOWING")
  val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

  // 1. 현재 테마의 텍스트 색상을 가져오고, textStyle에 색상이 없으면 이를 적용합니다.
  val textColor = textStyle.color.takeOrElse {
    colors.focusedTextColor // 또는 MaterialTheme.colorScheme.onSurface
  }
  val mergedTextStyle = textStyle.copy(color = textColor)

  CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
    BasicTextField(
      value = value,
      modifier = modifier,
      onValueChange = onValueChange,
      enabled = enabled,
      readOnly = readOnly,
      textStyle = mergedTextStyle,
      cursorBrush = SolidColor(colors.cursorColor),
      visualTransformation = visualTransformation,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      interactionSource = interactionSource,
      singleLine = singleLine,
      maxLines = maxLines,
      minLines = minLines,
      decorationBox =
        @Composable { innerTextField ->
          OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            visualTransformation = visualTransformation,
            innerTextField = innerTextField,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            container = {
              OutlinedTextFieldDefaults.Container(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                shape = shape,
              )
            }
          )
        }
    )
  }
}

// TextFieldValue버전의 CustomTextField. value와 onValueChange의 값이 다르다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = LocalTextStyle.current,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  prefix: @Composable (() -> Unit)? = null,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null,
  isError: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  minLines: Int = 1,
  interactionSource: MutableInteractionSource? = null,
  shape: Shape = OutlinedTextFieldDefaults.shape,
  colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
  contentPadding: PaddingValues = PaddingValues(2.dp)
) {
  @Suppress("NAME_SHADOWING")
  val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

  // 1. 현재 테마의 텍스트 색상을 가져오고, textStyle에 색상이 없으면 이를 적용합니다.
  val textColor = textStyle.color.takeOrElse {
    colors.focusedTextColor // 또는 MaterialTheme.colorScheme.onSurface
  }
  val mergedTextStyle = textStyle.copy(color = textColor)

  CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
    BasicTextField(
      value = value,
      modifier = modifier,
      onValueChange = onValueChange,
      enabled = enabled,
      readOnly = readOnly,
      textStyle = mergedTextStyle,
      cursorBrush = SolidColor(colors.cursorColor),
      visualTransformation = visualTransformation,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      interactionSource = interactionSource,
      singleLine = singleLine,
      maxLines = maxLines,
      minLines = minLines,
      decorationBox =
        @Composable { innerTextField ->
          OutlinedTextFieldDefaults.DecorationBox(
            value = value.text,
            visualTransformation = visualTransformation,
            innerTextField = innerTextField,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            container = {
              OutlinedTextFieldDefaults.Container(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                shape = shape,
              )
            }
          )
        }
    )
  }
}