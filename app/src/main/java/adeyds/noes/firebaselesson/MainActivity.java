package adeyds.noes.firebaselesson;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import adeyds.noes.firebaselesson.adapter.TodoListAdapter;
import adeyds.noes.firebaselesson.helper.SimpleDividerItemDecoration;
import adeyds.noes.firebaselesson.model.Todo;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TABLE_TODOS = "users/";
    //nama tabel
    private static final String APP_TITLE = "app_title";
    private static final String IS_DONE = "is_done";

    private RecyclerView listTodo;
    private EditText edtTodo;
    private ProgressBar loading;
    private TextView txtEmptyInfo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnLogout;

    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private DatabaseReference connectedRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference myConnectionsRef;

    private TodoListAdapter adapter;
    private LinearLayoutManager linearLayoutManager;
    private LoginActivity loginActivity;
    private String todoItem, todoId, noTelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noTelp = getIntent().getStringExtra("NOTELP");
        listTodo = findViewById(R.id.listTodo);
        edtTodo = findViewById(R.id.edtTodo);
        loading = findViewById(R.id.loading);
        txtEmptyInfo = findViewById(R.id.txtEmptyInfo);
        swipeRefreshLayout = findViewById(R.id.swiper);
        btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginActivity = new LoginActivity(true);
                finishAffinity();
            }
        });

        adapter = new TodoListAdapter(this);
        linearLayoutManager = new LinearLayoutManager(this);

        listTodo.setAdapter(adapter);
        listTodo.setLayoutManager(linearLayoutManager);
        listTodo.addItemDecoration(new SimpleDividerItemDecoration(this));

        mFirebaseInstance = FirebaseDatabase.getInstance();

        mFirebaseDatabase = mFirebaseInstance.getReference(TABLE_TODOS + noTelp + "/todos");

        lastOnlineRef = mFirebaseInstance.getReference("/users/" + noTelp + "/lastOnline");

        connectedRef = mFirebaseInstance.getReference(".info/connected");

        myConnectionsRef = mFirebaseInstance.getReference("users/" + noTelp + "/connections");
        connectRef();
        mFirebaseDatabase.keepSynced(true);

        mFirebaseInstance.getReference(APP_TITLE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "App title updated");
                String appTitle = dataSnapshot.getValue(String.class);
                getSupportActionBar().setTitle(appTitle);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.e(TAG, "Failed to read app title value.", error.toException());
            }
        });

        edtTodo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edtTodo.getWindowToken(), 0);

                    todoId = mFirebaseDatabase.push().getKey();
                    todoItem = edtTodo.getText().toString();

                    if (todoItem == "" || todoItem.equals("")) {
                        edtTodo.setError("Couldn't add empty todo!");
                    } else {
                        // edtTodo.setText(TAG);
                        addTodo(todoId, todoItem, false);
                    }

                    handled = true;
                }

                return handled;
            }
        });

        //  loading.setVisibility(View.VISIBLE);
        loadTodoList();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                loadTodoList();
            }
        });

        adapter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Todo todo = adapter.getData().get(position);
                mFirebaseDatabase.child(todo.id).setValue(null);

                return true;
            }
        });

    }

    private void connectRef() {
        connectedRef.addValueEventListener(new ValueEventListener() {
                                               @Override
                                               public void onDataChange(DataSnapshot snapshot) {
                                                   boolean connected = snapshot.getValue(Boolean.class);
                                                   if (connected) {
                                                       DatabaseReference con = myConnectionsRef.push();

                                                       // when this device disconnects, remove it
                                                       con.onDisconnect().removeValue();

                                                       // when I disconnect, update the last time I was seen online
                                                       lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);

                                                       // add this device to my connections list
                                                       // this value could contain info about the device or a timestamp too
                                                       con.setValue(Boolean.TRUE);

                                                   }
                                               }

                                               @Override
                                               public void onCancelled(DatabaseError databaseError) {

                                               }
                                           }
        );
    }

    public void setDone(String id, boolean isDone) {
        mFirebaseDatabase.child(id).child(IS_DONE).setValue(isDone);
    }

    private void loadTodoList() {
        mFirebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                adapter.getData().clear();

                if (dataSnapshot.getChildrenCount() != 0) {
                    txtEmptyInfo.setVisibility(View.GONE);

                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Todo todo = data.getValue(Todo.class);
                        adapter.getData().add(todo);
                        adapter.notifyItemInserted(adapter.getData().size() - 1);
                    }
                    adapter.notifyDataSetChanged();

                } else {
                    txtEmptyInfo.setVisibility(View.VISIBLE);
                }
                swipeRefreshLayout.setRefreshing(false);
                //  loading.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addTodo(String todoId, String todoItem, boolean isDone) {
        Todo todo = new Todo(todoId, todoItem, isDone);
        mFirebaseDatabase.child(todoId).setValue(todo);
        edtTodo.setText("");
    }

}

