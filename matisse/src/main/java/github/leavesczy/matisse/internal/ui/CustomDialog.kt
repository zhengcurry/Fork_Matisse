package github.leavesczy.matisse.internal.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.R

/**
 * 删除确认对话框。
 *
 * 使用同窗口全屏遮罩层替代 Dialog()，避免创建独立 Window 导致的：
 * - 导航栏/状态栏闪现（独立 Window 不继承 Activity 的沉浸式设置）
 * - 布局跳变（decorFitsSystemWindows 切换导致可用空间变化）
 */
@Composable
fun CustomButtonDialog(
    @StringRes text: Int,
    onDismissRequest: () -> Unit,
    onSureClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    // 全屏半透明遮罩（同一 Window 内，不创建新 Window）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.5f)
                .fillMaxHeight(fraction = 0.55f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* 拦截点击，防止穿透到遮罩层 */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.matisse_dialog_bg_color),
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(text),
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                        .padding(10.dp),
                ) {
                    Button(
                        onSureClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.matisse_dialog_positive_btn_color)
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.matisse_dialog_sure),
                            color = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Button(
                        onCancelClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.matisse_dialog_negative_btn_color)
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.matisse_cancel),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 1440,
    heightDp = 810
)
@Composable
fun CustomDialogPreview() {
    CustomButtonDialog(R.string.matisse_preview, onDismissRequest = {}, {}, {})
}
