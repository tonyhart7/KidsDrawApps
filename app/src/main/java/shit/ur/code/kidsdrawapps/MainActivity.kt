package shit.ur.code.kidsdrawapps

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.loader.content.AsyncTaskLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialogbrushsize.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageBtnCurentpaint: ImageButton? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeBrush(10.toFloat())

        mImageBtnCurentpaint = ll_color_palete [1] as ImageButton
        mImageBtnCurentpaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        ib_brush.setOnClickListener{
            showBrushSizeDialog()
        }
        gallery_btn.setOnClickListener{
            if (isReadStorageAllowed()){

                val picPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(picPhotoIntent, GALLERY)

            }else{
                requestStoragePermission()
            }
        }

        undo_btn.setOnClickListener{
            drawing_view.onClickUndo()
        }

        save_btn.setOnClickListener{
            if (isReadStorageAllowed()){
                BitmapAsyncTask(getBitmanfrmView(Frame_ll)).execute()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY){
                try {
                    if (data!!.data != null){
                        bg_frame.visibility = View.VISIBLE
                        bg_frame.setImageURI(data.data)
                    }else{
                        Toast.makeText(this@MainActivity, "Error Parsing or Image is Corrupted", Toast.LENGTH_SHORT).show()
                    }

                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeDialog(){

        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialogbrushsize)
        brushDialog.setTitle("Brush Size: ")
        
        val smallBtn = brushDialog.ib_smallBrush
        smallBtn.setOnClickListener{
            drawing_view.setSizeBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.ib_mediumBrush
        mediumBtn.setOnClickListener{
            drawing_view.setSizeBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.ib_largeBrush
        largeBtn.setOnClickListener{
            drawing_view.setSizeBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun painClicked(view: View){
        if(view !== mImageBtnCurentpaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageBtnCurentpaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageBtnCurentpaint = view
        }
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this, "Need a Permission to add Background", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this@MainActivity, "You have Access to Storage", Toast.LENGTH_LONG).show()
            }
        }else{
            Toast.makeText(this@MainActivity, "You denied Permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmanfrmView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap): ViewModel(){

        fun execute() = viewModelScope.launch {

            onPreExecute()

            val result = doInBackground()

            onPostExecute(result)

        }

        private lateinit var mProgressDialog: Dialog

        private fun onPreExecute() {

            showProgressDialog()

        }

        private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {

            var result = ""

            if(mBitmap != null){

                try{

                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")

                    val fos = FileOutputStream(f)

                    fos.write(bytes.toByteArray())

                    fos.close()

                    result = f.absolutePath

                } catch (e: Exception){

                    result = ""

                    e.printStackTrace()

                }

            }

            return@withContext result

        }

        private fun onPostExecute(result: String?) {

            cancelDialog()

            if(!result!!.isEmpty()) {

                Toast.makeText(

                    this@MainActivity,

                    "File Saved Succesfully : $result",

                    Toast.LENGTH_SHORT

                ).show()

            } else {

                Toast.makeText(this@MainActivity,

                    "Something went wrong while saving file",

                    Toast.LENGTH_SHORT).show()

            }

            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){

                    path, uri -> val shareIntent = Intent()

                shareIntent.action = Intent.ACTION_SEND

                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)

                shareIntent.type = "image/png"

                startActivity(

                    Intent.createChooser(

                        shareIntent, "Share"

                    )

                )

            }

        }

        private fun showProgressDialog(){

            mProgressDialog = Dialog(this@MainActivity)

            mProgressDialog.setContentView(R.layout.dialog_custom_progress)

            mProgressDialog.show()

        }

        private fun cancelDialog(){

            mProgressDialog.dismiss()

        }

    }

    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}