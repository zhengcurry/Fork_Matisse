package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.R

/**
 *
 *
 *
 * @author : curry
 *
 * create on 2025/9/25
 *
 */
@Composable
internal fun MediaPreviewTopBar(
    modifier: Modifier,
    onClickDelete: () -> Unit,
    backEvent: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        // 左右两边组件
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                modifier = Modifier
                    .clickableNoRipple {
                        backEvent()
                    }
                    .padding(end = 12.dp)
                    .fillMaxHeight()
                    .size(size = 32.dp),
                painter = painterResource(id = R.drawable.icon_back),
                tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                contentDescription = null
            )

            Button(
                onClick = {
                    onClickDelete()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.matisse_type_delete_btn_color)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    stringResource(R.string.matisse_delete),
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaPreviewTopBarPreview() {
    MediaPreviewTopBar(
        modifier = Modifier,
        onClickDelete = { },
        {}
    )
}
