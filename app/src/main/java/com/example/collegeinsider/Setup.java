package com.example.collegeinsider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class Setup extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageUri=null;
    private EditText setupName;
    private Button setupBtn;
    private String user_id;


    private StorageReference mStorageRef;
    private FirebaseAuth firebaseAuth;
    private ProgressBar setupProgress;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar=findViewById(R.id.setupActivityToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setup");

        setupImage=findViewById(R.id.setup_image);
        setupName=findViewById(R.id.setup_name);
        setupBtn=findViewById(R.id.setup_btn);
        setupProgress=findViewById(R.id.setup_progressBar);

        firebaseAuth=FirebaseAuth.getInstance();
        mStorageRef= FirebaseStorage.getInstance().getReference();
        db= FirebaseFirestore.getInstance();
        user_id=firebaseAuth.getCurrentUser().getUid();

        setupProgress.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);
        db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> userData=document.getData();
                        if(user_id.equals(userData.get("user_id"))){
                            Log.v("DBTag",  " => " + userData.get("user_image"));
                            setupName.setText((String)userData.get("user_name"));
                            String image=(String)userData.get("user_image");
                            mainImageUri=Uri.parse(image);
                            Glide.with(Setup.this).load(image).into(setupImage);
                        }
                    }

                }
                else{
                    String errorMsg=task.getException().getMessage();
                    Toast.makeText(Setup.this,"Error: "+errorMsg,Toast.LENGTH_SHORT).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
                setupBtn.setEnabled(true);
            }
        });

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String userName=setupName.getText().toString();
                if(!TextUtils.isEmpty(userName) && mainImageUri!=null){
                    user_id=firebaseAuth.getCurrentUser().getUid();
                    setupProgress.setVisibility(View.VISIBLE);
                    StorageReference image_path=mStorageRef.child("profile_pics").child(user_id+".jpg");
                    image_path.putFile(mainImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                            if (taskSnapshot.getMetadata() != null) {
                                if (taskSnapshot.getMetadata().getReference() != null) {
                                    Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                    result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
//                      Toast.makeText(Setup.this,"URL generated!",Toast.LENGTH_SHORT).show();
                                            String imageUrl = uri.toString();
                                            Map<String,String> userMap=new HashMap<>();
                                            userMap.put("user_id",user_id);
                                            userMap.put("user_name",userName);
                                            userMap.put("user_image",imageUrl);

                                            db.collection("Users").add(userMap).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Toast.makeText(Setup.this,"Updated!",Toast.LENGTH_SHORT).show();
                                                    Intent intent=new  Intent(Setup.this,MainActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    String errorMsg=e.getMessage();
                                                    Toast.makeText(Setup.this,"Error:",Toast.LENGTH_SHORT).show();
                                                    Toast.makeText(Setup.this,"Error: "+errorMsg,Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                        }
                                    });
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String errorMsg= e.getMessage();
                            Toast.makeText(Setup.this,"Error: "+errorMsg,Toast.LENGTH_SHORT).show();
                            setupProgress.setVisibility(View.INVISIBLE);

                        }
                    });
                }
                else{
                    Toast.makeText(Setup.this,"Please select all required items",Toast.LENGTH_SHORT).show();
                }
            }
        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                    if(ContextCompat.checkSelfPermission(Setup.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(Setup.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    }
                    if(ContextCompat.checkSelfPermission(Setup.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(Setup.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                    }
                    else{
                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setAspectRatio(1,1)
                                .start(Setup.this);
                        Toast.makeText(Setup.this,"Storage access allowed",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
               mainImageUri = result.getUri();
               setupImage.setImageURI(mainImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(Setup.this,error.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }

}