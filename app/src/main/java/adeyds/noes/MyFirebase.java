package adeyds.noes;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class MyFirebase extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
