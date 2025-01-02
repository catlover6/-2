package com.example.ranomselectapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ranomselectapp.ui.theme.RanomSelectAppTheme
import java.io.File
import android.content.Context
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) 앱 실행 시, 원본 파일이 없으면 복사
        copyReligionRawToInternalIfNeeded()

        setContent {
            RanomSelectAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RandomRegionScreen(
                        onShuffleClick = { shuffleFile() },
                        onSelectClick = { selectOneFromShuffled() },
                        onReadOriginalAll = { readReligionListAll() },
                        onWriteOriginalAll = { newContent -> writeReligionListAll(newContent) },
                        onResetOriginal = { resetOriginalFile() },
                        // shuffle.txt 읽고 쓰는 메서드를 콜백으로 넘겨줌
                        onReadShuffleAll = { readShuffleAll() },
                        onWriteShuffleAll = { newContent -> writeShuffleAll(newContent) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * (A) 첫 실행 시점 등, 내부 저장소에 religion_list.txt가 없으면
     *     raw/religion_list.txt를 복사.
     */
    private fun copyReligionRawToInternalIfNeeded() {
        val internalFile = File(filesDir, "religion_list.txt")
        if (!internalFile.exists()) {
            val inputStream = resources.openRawResource(R.raw.religion_list)
            val content = inputStream.bufferedReader().use { it.readText() }
            openFileOutput("religion_list.txt", Context.MODE_PRIVATE).use { fos ->
                fos.write(content.toByteArray())
            }
        }
    }

    /**
     * (B) 내부 저장소의 religion_list.txt를 원본( raw ) 상태로 되돌리는 함수
     */
    private fun resetOriginalFile() {
        val inputStream = resources.openRawResource(R.raw.religion_list)
        val content = inputStream.bufferedReader().use { it.readText() }
        openFileOutput("religion_list.txt", Context.MODE_PRIVATE).use { fos ->
            fos.write(content.toByteArray())
        }
        Toast.makeText(this, "원본 파일로 초기화했습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * (C) Shuffle:
     *  - 내부 저장소의 religion_list.txt를 읽어 무작위 섞고, shuffled.txt에 저장
     */
    private fun shuffleFile() {
        val originalList = readReligionListFromInternal("religion_list.txt")
        val shuffledList = originalList.shuffled()
        writeShuffledListToInternal(shuffledList, "shuffled.txt")
        Toast.makeText(this, "Shuffled list 생성 완료!", Toast.LENGTH_SHORT).show()
    }

    /**
     * (D) Select:
     *  - shuffled.txt에서 맨 위 한 줄 꺼내 반환
     */
    private fun selectOneFromShuffled(): String {
        val lines = readShuffledListFromInternal("shuffled.txt").toMutableList()
        return if (lines.isNotEmpty()) {
            val selected = lines.removeAt(0)
            writeShuffledListToInternal(lines, "shuffled.txt")
            selected
        } else {
            "더 이상 동이 없습니다."
        }
    }

    /**
     * (E) 원본 파일(religion_list.txt) 읽어 문자열 전체 반환 (편집용)
     */
    private fun readReligionListAll(): String {
        val lines = readReligionListFromInternal("religion_list.txt")
        return lines.joinToString("\n")
    }

    /**
     * (F) 새 문자열을 받아, religion_list.txt를 덮어씀 (편집 저장)
     */
    private fun writeReligionListAll(newContent: String) {
        val list = newContent.split("\n")
        writeShuffledListToInternal(list, "religion_list.txt")
    }

    /**
     * (G) shuffle.txt 전체 읽어 문자열로 반환
     */
    private fun readShuffleAll(): String {
        val lines = readShuffledListFromInternal("shuffled.txt")
        return lines.joinToString("\n")
    }

    /**
     * (H) shuffle.txt에 새 문자열 덮어쓰기
     */
    private fun writeShuffleAll(newContent: String) {
        val list = newContent.split("\n")
        writeShuffledListToInternal(list, "shuffled.txt")
    }

    /**
     * 내부 저장소(filename) -> List<String>
     */
    private fun readReligionListFromInternal(filename: String): List<String> {
        val file = File(filesDir, filename)
        return if (file.exists()) file.readLines() else emptyList()
    }

    /**
     * 내부 저장소(filename) <- List<String>
     */
    private fun writeShuffledListToInternal(list: List<String>, filename: String) {
        openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
            fos.write(list.joinToString("\n").toByteArray())
        }
    }

    /**
     * shuffled.txt -> List<String>
     */
    private fun readShuffledListFromInternal(filename: String): List<String> {
        val file = File(filesDir, filename)
        return if (file.exists()) file.readLines() else emptyList()
    }
}

/**
 * Compose UI
 */
@Composable
fun RandomRegionScreen(
    onShuffleClick: () -> Unit,
    onSelectClick: () -> String,
    onReadOriginalAll: () -> String,
    onWriteOriginalAll: (String) -> Unit,
    onResetOriginal: () -> Unit,
    onReadShuffleAll: () -> String,
    onWriteShuffleAll: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRegion by remember { mutableStateOf("") }

    // 원본 편집 UI
    var showOriginalEditor by remember { mutableStateOf(false) }
    var originalEditorText by remember { mutableStateOf("") }

    // shuffle.txt 편집 UI
    var showShuffleEditor by remember { mutableStateOf(false) }
    var shuffleEditorText by remember { mutableStateOf("") }

    // Shuffle 버튼 연속 클릭 횟수
    var shuffleCount by remember { mutableStateOf(0) }

    Column(modifier = modifier.padding(16.dp)) {

        // 현재 선택된 항목 표시
        Text(
            text = if (selectedRegion.isEmpty()) {
                "아직 선택되지 않았습니다."
            } else {
                selectedRegion
            },
            fontSize = 35.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Shuffle 버튼
        Button(onClick = {
            // Shuffle 로직 호출
            onShuffleClick()
            // 연속 클릭 횟수 증가
            shuffleCount++
            // 10번 연속 누르면 shuffle.txt 편집 UI 표시
            if (shuffleCount == 10) {
                showShuffleEditor = true
                // shuffle.txt 전체 읽어오기
                shuffleEditorText = onReadShuffleAll()
            }
        }) {
            Text(text = "Shuffle")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Select 버튼
        Button(onClick = {
            val result = onSelectClick()
            selectedRegion = result
            // Select 버튼 누르면 shuffleCount 리셋
            shuffleCount = 0
        }) {
            Text(text = "Select")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 원본(Edit) 버튼
        Button(onClick = {
            originalEditorText = onReadOriginalAll()
            showOriginalEditor = true
            // 다른 버튼 클릭 시 shuffleCount 리셋
            shuffleCount = 0
        }) {
            Text(text = "Edit Original")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 원본(Reset) 버튼
        Button(onClick = {
            onResetOriginal()
            showOriginalEditor = false
            shuffleCount = 0
        }) {
            Text(text = "Reset to Original")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 만약 shuffle.txt 편집 UI가 열려있다면 표시
        if (showShuffleEditor) {
            Text(text = "shuffle.txt 내용 (직접 수정 후 Save)")

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = shuffleEditorText,
                onValueChange = { shuffleEditorText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("shuffle.txt") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = {
                    onWriteShuffleAll(shuffleEditorText)
                    showShuffleEditor = false
                    shuffleCount = 0
                }) {
                    Text("Save")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = {
                    showShuffleEditor = false
                    shuffleCount = 0
                }) {
                    Text("Cancel")
                }
            }
        }

        // 원본 파일 편집 UI
        if (showOriginalEditor) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "원본 파일 내용 (직접 수정 후 Save)")

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = originalEditorText,
                onValueChange = { originalEditorText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("religion_list.txt") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = {
                    onWriteOriginalAll(originalEditorText)
                    showOriginalEditor = false
                    shuffleCount = 0
                }) {
                    Text("Save")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = {
                    showOriginalEditor = false
                    shuffleCount = 0
                }) {
                    Text("Cancel")
                }
            }
        }
    }
}
