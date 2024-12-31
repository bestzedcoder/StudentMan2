package vn.edu.hust.studentman

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student")
data class StudentEntity(
    @PrimaryKey
    val studentId: String,
    val studentName: String
)
