package adeyds.noes.firebaselesson;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private EditText edtTelp, edtVerif;
    private Button btnSend, btnVerif;

    //kebutuhan
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener stateListener;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String VerifikasiID;
    private String No_Telepon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        edtTelp = findViewById(R.id.edt_telp);
        edtVerif = findViewById(R.id.edt_verif);
        btnSend = findViewById(R.id.btn_send);
        btnVerif= findViewById(R.id.btn_resend);
        auth = FirebaseAuth.getInstance();
        stateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //Meneteksi Apakah Ada User Yang Sedang Login (Belum Logout)
                if (user != null) {
                    //Jika Ada, User Tidak perlu Login Lagi, dan Langsung Menuju Acivity Yang Dituju
                    startActivityForResult(new Intent(LoginActivity.this, MainActivity.class), 900);

                }
            }
        };


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                No_Telepon = "+62"+edtTelp.getText().toString();
                setupVerificationCallback();
              // PhoneAuthProvider.getInstance().verifyPhoneNumber(No_Telepon, 60, TimeUnit.SECONDS, this, callbacks);
                verifyPhone(No_Telepon,callbacks);
                Toast.makeText(getApplicationContext(), "Memverifikasi, Mohon Tunggu", Toast.LENGTH_SHORT).show();
                edtTelp.setText("");
            }
        });

        btnVerif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String verifiCode = edtVerif.getText().toString();
                if(TextUtils.isEmpty(verifiCode)){
                    //Memberi Pesan pada user bahwa kode verifikasi tidak boleh kosong saat menekan Tombol Verifikasi
                    Toast.makeText(getApplicationContext(),"Masukan Kode Verifikasi", Toast.LENGTH_SHORT).show();
                }else{
                    //Memverifikasi Nomor Telepon, Saat Tombol Verifikasi Ditekan
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(VerifikasiID, verifiCode);
                    signInWithPhoneAuthCredential(credential);
                    Toast.makeText(getApplicationContext(),"Sedang Memverifikasi", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public LoginActivity(Boolean logout){
        if (logout){
            auth.getInstance().signOut();
        }
    }
    public LoginActivity(){

    }
    public void verifyPhone(String phoneNumber, PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks){
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbac
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Melampirkan Listener pada FirebaseAuth saat Activity Dimulai
        auth.addAuthStateListener(stateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (stateListener != null) {
            //Menghapus Listener pada FirebaseAuth saat Activity Dihentikan
            auth.removeAuthStateListener(stateListener);
        }
    }

    private void setupVerificationCallback() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                // Callback disini akan dipanggil saat Verifikasi Selseai atau Berhasil
                Toast.makeText(getApplicationContext(), "Verifikasi Selesai", Toast.LENGTH_SHORT).show();
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // Callback disini akan dipanggil saat permintaan tidak valid atau terdapat kesalahan
                Toast.makeText(getApplicationContext(), "Verifikasi Gagal, Silakan Coba Lagi", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                // Callback didalam sini akan dipanggil/dieksekusi saat terjadi proses pengiriman kode
                // Dan User Diminta untuk memasukan kode verifikasi

                // Untuk Menyimpan ID verifikasi dan kirim ulang token
                VerifikasiID = verificationId;
                Log.e("verif", VerifikasiID+" "+token.toString());
                resendToken = token;
                //   Resend.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Mendapatkan Kode Verifikasi", Toast.LENGTH_SHORT).show();
            }


        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){
        auth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            //Sign In Berhasil
                            startActivityForResult(new Intent(LoginActivity.this, MainActivity.class).putExtra("NOTELP", No_Telepon), 900);
                            finish();
                        }else{
                            //Sign In Gagal
                            if(task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                // Kode Yang Dimasukan tidal Valid.
                                Toast.makeText(getApplicationContext(), "Kode yang dimasukkan tidak valid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==900 ){
            Log.e("Login", "logout");
            finish();
        }else {
            finish();
        }
    }
}
