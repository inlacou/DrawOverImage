# DrawOverImage

[![](https://jitpack.io/v/inlacou/DrawOverImage.svg)](https://jitpack.io/#inlacou/DrawOverImage)

## Usage example

In kotlin:
```Kt
fun editImage(toEditImagePath: String){
  PictureDrawEditorAct.navigateForResult(this@ExampleActivity, REQUEST_CODE_EDIT, PictureDrawEditorMdl(toEditImagePath, isUiHideable = true, showForwardButton = true))
}

protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode, resultCode, data)
  if(requestCode==REQUEST_CODE_EDIT && resultCode==Activity.RESULT_OK && data!=null && data.hasExtra(PictureDrawEditorAct.RESULT_FILE_ABSOLUTE_PATH)){
    val path = data.getStringExtra(PictureDrawEditorAct.RESULT_FILE_ABSOLUTE_PATH)
  }
}
```
