package vn.edu.hust.studentman

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface StudentDao {
    // Lấy toàn bộ student
    @Query("SELECT * FROM student")
    fun getAllStudents(): List<StudentEntity>

    // Thêm 1 student
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addStudent(student: StudentEntity): Long

    // Cập nhật student (theo studentId)
    @Update
    fun updateStudent(student: StudentEntity): Int

    // Xóa student
    @Delete
    fun deleteStudent(student: StudentEntity): Int

    // Nếu muốn xóa theo khóa chính thủ công
    @Query("DELETE FROM student WHERE studentId = :id")
    fun deleteStudentById(id: String): Int
}
