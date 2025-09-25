package github.leavesczy.matisse.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
internal fun MediaTypeTopBar(
    modifier: Modifier,
    onClickMediaType: (Int) -> Unit,
    onClickChoice: (Boolean) -> Unit,
    onClickDelete: () -> Unit,
) {
    val options = listOf(R.string.matisse_image, R.string.matisse_video)
    var selectedIndex by remember { mutableIntStateOf(0) }
    var choice by remember { mutableStateOf(false) }

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
                    }
                    .padding(start = 18.dp, end = 12.dp)
                    .fillMaxHeight()
                    .size(size = 32.dp),
                painter = painterResource(id = R.drawable.icon_back),
                tint = colorResource(id = R.color.matisse_top_bar_icon_color),
                contentDescription = null
            )

            if (!choice) {
                Button(
                    onClick = {
                        choice = !choice
                        onClickChoice(choice)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.matisse_type_common_btn_color)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        stringResource(R.string.matisse_choice),
                        color = Color.White
                    )
                }
            }

            if (choice) {
                Row {
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            choice = !choice
                            onClickChoice(choice)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.matisse_type_common_btn_color)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.matisse_cancel),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxHeight(fraction = 0.76f)
                .fillMaxWidth(fraction = 0.3f)
                .align(Alignment.Center)
                .background(color = colorResource(id = R.color.matisse_type_bg_color), RoundedCornerShape(28.dp)),
        ) {
            options.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedIndex == index)
                                colorResource(id = R.color.matisse_type_switch_bg_color)
                            else
                                Color.Transparent,
                            RoundedCornerShape(28.dp)
                        )
                        .fillMaxHeight()
                        .clickable {
                            selectedIndex = index
                            onClickMediaType(selectedIndex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(label),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaTypeTopBarPreview() {
    MediaTypeTopBar(
        modifier = Modifier,
        onClickMediaType = { },
        onClickChoice = { },
        onClickDelete = { }
    )
}
