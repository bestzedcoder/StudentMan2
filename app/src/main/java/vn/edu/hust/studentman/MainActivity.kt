package vn.edu.hust.studentman

import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

  private lateinit var studentAdapter: StudentAdapter
  private var deletedStudent: StudentModel? = null
  private var deletedPosition: Int = -1

  // Thay vì DatabaseHelper, ta dùng AppDatabase
  private lateinit var appDatabase: AppDatabase
  private lateinit var studentDao: StudentDao

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Lấy instance của DB & DAO
    appDatabase = AppDatabase.getInstance(this)
    studentDao = appDatabase.studentDao()

    // Lấy danh sách từ DB (dạng StudentEntity), rồi chuyển sang StudentModel
    val studentEntities = studentDao.getAllStudents()
    val students = studentEntities.map { entity ->
      StudentModel(entity.studentName, entity.studentId)
    }.toMutableList()

    // Tạo adapter
    studentAdapter = StudentAdapter(students) { student, position, action ->
      when (action) {
        "edit" -> showEditDialog(student, position)
        "delete" -> showDeleteDialog(student, position)
      }
    }

    findViewById<RecyclerView>(R.id.recycler_view_students).apply {
      adapter = studentAdapter
      layoutManager = LinearLayoutManager(this@MainActivity)
    }

    findViewById<Button>(R.id.btn_add_new).setOnClickListener {
      showAddDialog()
    }
  }

  // Dialog thêm SV
  private fun showAddDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_student, null)

    AlertDialog.Builder(this)
      .setTitle("Thêm sinh viên mới")
      .setView(dialogView)
      .setPositiveButton("Thêm") { _, _ ->
        val name = dialogView.findViewById<EditText>(R.id.edit_text_name).text.toString()
        val studentId = dialogView.findViewById<EditText>(R.id.edit_text_student_id).text.toString()

        if (name.isNotEmpty() && studentId.isNotEmpty()) {
          // Thêm vào DB dưới dạng StudentEntity
          val entity = StudentEntity(studentId = studentId, studentName = name)
          val rowId = studentDao.addStudent(entity)

          if (rowId != -1L) {
            // Thêm vào adapter (StudentModel)
            val newStudent = StudentModel(name, studentId)
            studentAdapter.addStudent(newStudent)
          }
        }
      }
      .setNegativeButton("Hủy", null)
      .show()
  }

  // Dialog sửa SV
  private fun showEditDialog(student: StudentModel, position: Int) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_student, null)
    val editName = dialogView.findViewById<EditText>(R.id.edit_text_name)
    val editStudentId = dialogView.findViewById<EditText>(R.id.edit_text_student_id)

    editName.setText(student.studentName)
    editStudentId.setText(student.studentId)

    AlertDialog.Builder(this)
      .setTitle("Sửa thông tin sinh viên")
      .setView(dialogView)
      .setPositiveButton("Cập nhật") { _, _ ->
        val newName = editName.text.toString()
        val newId = editStudentId.text.toString()
        val oldId = student.studentId

        if (newName.isNotEmpty() && newId.isNotEmpty()) {
          // Tạo entity mới
          val entity = StudentEntity(studentId = newId, studentName = newName)

          // Xoá entity cũ hoặc update cũ. Tùy logic:
          // 1. Cách 1: xóa cũ -> thêm mới
          // studentDao.deleteStudentById(oldId)
          // studentDao.addStudent(entity)

          // 2. Cách 2: Đổi trực tiếp primary key (có thể phức tạp vì xung đột)
          // 3. Cách 3: Chỉ update name, ID không đổi => dễ nhất
          //    => Vậy ta cho ID làm cột fix, name có thể sửa. Tùy vào bạn.

          // Ở đây, giả sử ta update theo primary key cũ:
          // (Room không hỗ trợ update primary key "một cách trực tiếp"
          //  trừ phi xóa và thêm record mới.)
          // => ta xóa record cũ => thêm record mới
          if (newId != oldId) {
            studentDao.deleteStudentById(oldId)
            studentDao.addStudent(entity)
          } else {
            // ID không đổi -> update
            studentDao.updateStudent(entity)
          }

          // Cập nhật adapter
          val updatedStudent = StudentModel(newName, newId)
          studentAdapter.updateStudent(updatedStudent, position)
        }
      }
      .setNegativeButton("Hủy", null)
      .show()
  }

  // Dialog xác nhận xóa
  private fun showDeleteDialog(student: StudentModel, position: Int) {
    AlertDialog.Builder(this)
      .setTitle("Xác nhận xóa")
      .setMessage("Bạn có chắc chắn muốn xóa sinh viên ${student.studentName}?")
      .setPositiveButton("Xóa") { _, _ ->
        deleteStudent(position)
      }
      .setNegativeButton("Hủy", null)
      .show()
  }

  // Xóa SV
  private fun deleteStudent(position: Int) {
    val studentToDelete = studentAdapter.getStudent(position)
    val entity = StudentEntity(
      studentId = studentToDelete.studentId,
      studentName = studentToDelete.studentName
    )
    val rowsDeleted = studentDao.deleteStudent(entity)
    if (rowsDeleted > 0) {
      deletedStudent = studentAdapter.removeStudent(position)
      deletedPosition = position

      Snackbar.make(
        findViewById(R.id.main),
        "Đã xóa ${deletedStudent?.studentName}",
        Snackbar.LENGTH_LONG
      ).setAction("Hoàn tác") {
        deletedStudent?.let { undoneStudent ->
          val undoneEntity = StudentEntity(
            studentId = undoneStudent.studentId,
            studentName = undoneStudent.studentName
          )
          val rowId = studentDao.addStudent(undoneEntity)
          if (rowId != -1L) {
            studentAdapter.addStudent(undoneStudent)
          }
          deletedStudent = null
          deletedPosition = -1
        }
      }.show()
    }
  }

  // Menu option (nếu bạn có)
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.option_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_add_new -> {
        showAddDialog()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  // Menu context (nếu bạn có)
  override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
    super.onCreateContextMenu(menu, v, menuInfo)
    menuInflater.inflate(R.menu.context_menu, menu)
  }

  override fun onContextItemSelected(item: MenuItem): Boolean {
    val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
    val position = info.position
    return when (item.itemId) {
      R.id.menu_edit -> {
        val student = studentAdapter.getStudent(position)
        showEditDialog(student, position)
        true
      }
      R.id.menu_remove -> {
        val student = studentAdapter.getStudent(position)
        showDeleteDialog(student, position)
        true
      }
      else -> super.onContextItemSelected(item)
    }
  }
}
