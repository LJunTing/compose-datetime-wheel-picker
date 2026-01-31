package dev.darkokoa.datetimewheelpicker.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.DateField
import dev.darkokoa.datetimewheelpicker.core.format.DateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

@Composable
internal fun StandardWheelDatePicker(
  modifier: Modifier = Modifier,
  startDate: LocalDate = LocalDate.now(),
  minDate: LocalDate = LocalDate.EPOCH,
  maxDate: LocalDate = LocalDate.CYB3R_1N1T_ZOLL,
  yearsRange: IntRange? = IntRange(minDate.year, maxDate.year),
  dateFormatter: DateFormatter = dateFormatter(
    locale = Locale.current,
    monthDisplayStyle = MonthDisplayStyle.SHORT,
    cjkSuffixConfig = CjkSuffixConfig.HideAll
  ),
  size: DpSize = DpSize(256.dp, 128.dp),
  rowCount: Int = 3,
  textStyle: TextStyle = MaterialTheme.typography.titleMedium,
  textColor: Color = LocalContentColor.current,
  selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
  onSnappedDate: (snappedDate: SnappedDate) -> Int? = { _ -> null }
) {
  val itemCount = if (yearsRange == null) 2 else 3
  val itemWidth = size.width / itemCount

    // 使用 MutableState 保存当前日期，并确保初始值在范围内
    val snappedDateM = remember {
        mutableStateOf(startDate.coerceIn(minDate, maxDate))
    }
    val snappedDate = snappedDateM.value

  val dayOfMonths =
    rememberFormattedDayOfMonths(snappedDate.month.number, snappedDate.year, dateFormatter)

  val months = rememberFormattedMonths(size.width, dateFormatter)

  val years = rememberFormattedYears(yearsRange, dateFormatter)

    val scop= rememberCoroutineScope ()

    val dayLazyListState: LazyListState = rememberLazyListState(dayOfMonths?.find { it.value == startDate.day }?.index ?: 0)
    val monthLazyListState: LazyListState = rememberLazyListState(months?.find { it.value == startDate.month.number }?.index ?: 0)
    val yearLazyListState: LazyListState = rememberLazyListState(years?.find { it.value == startDate.year }?.index ?: 0)

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (selectorProperties.enabled().value) {
      Surface(
        modifier = Modifier
          .size(size.width, size.height / rowCount),
        shape = selectorProperties.shape().value,
        color = selectorProperties.color().value,
        border = selectorProperties.border().value
      ) {}
    }
    Row {
      dateFormatter.dateOrder.fields.forEach { dateField ->
        when (dateField) {
          DateField.DAY -> {
            WheelTextPicker(
              size = DpSize(
                width = itemWidth,
                height = size.height
              ),
              texts = dayOfMonths.map { it.text },
              rowCount = rowCount,
              style = textStyle,
              color = textColor,
              selectorProperties = WheelPickerDefaults.selectorProperties(
                enabled = false
              ),
              startIndex = dayOfMonths.find { it.value == startDate.day }?.index ?: 0,
              onScrollFinished = { snappedIndex ->
                  val targetDay = dayOfMonths.find { it.index == snappedIndex }?.value ?: snappedDate.day
                  // 修正日期：如果该月没有这一天，withDayOfMonth 会自动抛出异常或处理，
                  // 这里建议先 coerce，确保日期合法
                  val newDate = snappedDate.withDayOfMonth(
                      targetDay.coerceIn(1, snappedDate.lengthOfMonth)
                                                          ).coerceIn(minDate, maxDate)

                  snappedDateM.value = newDate

                  val finalIndex = dayOfMonths.find { it.value == newDate.day }?.index
                  finalIndex?.let {
                      onSnappedDate(SnappedDate.DayOfMonth(newDate, it))
                  }
                  return@WheelTextPicker finalIndex
              }
            )
          }

          DateField.MONTH -> {
            WheelTextPicker(
              size = DpSize(
                width = itemWidth,
                height = size.height
              ),
              texts = months.map { it.text },
              rowCount = rowCount,
              style = textStyle,
              color = textColor,
              selectorProperties = WheelPickerDefaults.selectorProperties(
                enabled = false
              ),
              startIndex = months.find { it.value == startDate.month.number }?.index ?: 0,
              onScrollFinished = { snappedIndex ->
                  val targetMonth = months.find { it.index == snappedIndex }?.value ?: snappedDate.month.number

                  // 处理月份切换时可能的天数溢出（例如从31号切到2月）
                  val tempDate = snappedDate.withMonthNumber(targetMonth)
                  val newDate = tempDate.withDayOfMonth(
                      snappedDate.day.coerceIn(1, tempDate.lengthOfMonth)
                                                       ).coerceIn(minDate, maxDate)

                  snappedDateM.value = newDate

                  val finalIndex = months.find { it.value == newDate.month.number }?.index
                  finalIndex?.let {
                      onSnappedDate(SnappedDate.Month(newDate, it))
                  }

                  dayOfMonths.find { it.value == newDate.day }?.index?.let {day->
                      scop.launch {
                          dayLazyListState.scrollToItem(day)
                      }
                  }
                  return@WheelTextPicker finalIndex
              }
            )
          }

          DateField.YEAR -> {
            years?.let { years ->
              WheelTextPicker(
                size = DpSize(
                  width = itemWidth,
                  height = size.height
                ),
                texts = years.map { it.text },
                rowCount = rowCount,
                style = textStyle,
                color = textColor,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                  enabled = false
                ),
                startIndex = years.find { it.value == startDate.year }?.index ?: 0,
                onScrollFinished = { snappedIndex ->
                    val targetYear = years.find { it.index == snappedIndex }?.value ?: snappedDate.year

                    val tempDate = snappedDate.withYear(targetYear)
                    val newDate = tempDate.withDayOfMonth(
                        snappedDate.day.coerceIn(1, tempDate.lengthOfMonth)
                                                         ).coerceIn(minDate, maxDate)

                    snappedDateM.value = newDate

                    val finalIndex=years.find { it.value == newDate.year }?.index?.let {
                        onSnappedDate(SnappedDate.Year(newDate, it))
                    }

                    months.find { it.value == newDate.month.number }?.index?.let { month->
                        scop.launch {
                            monthLazyListState.scrollToItem(month)
                        }
                    }

                    dayOfMonths.find { it.value == newDate.day }?.index?.let {day->
                        scop.launch {
                            dayLazyListState.scrollToItem(day)
                        }
                    }
                    return@WheelTextPicker finalIndex
                }
              )
            }
          }
        }
      }
    }
  }
}