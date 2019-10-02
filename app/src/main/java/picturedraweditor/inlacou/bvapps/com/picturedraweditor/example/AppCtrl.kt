package picturedraweditor.inlacou.bvapps.com.picturedraweditor.example

import android.app.Application
import timber.log.Timber

class AppCtrl: Application() {
	
	init {
		Timber.plant(Timber.DebugTree())
	}
	
}