package com.example.zhl.notedemo.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.zhl.notedemo.R;
import com.example.zhl.notedemo.db.NoteDb;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Button note_new,cancel,choseAll,delete;
    private ListView mListView;
    private MyAdapter adapter;
    private NoteDb noteDb;
    private Cursor cursor;
    private static String[] PROJECTION = new String[]{"title","date"};
    public static Map<Long,Boolean> recordCursorIdStatus = new HashMap<Long,Boolean>();
    public static Map<Integer,Boolean> recordStatus = new HashMap<Integer,Boolean>();

    private LinearLayout mLinearLayout;
    private int count;
    private Long selectId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cancel = (Button) findViewById(R.id.cancel);
        choseAll = (Button) findViewById(R.id.chose_all);
        delete = (Button) findViewById(R.id.delete);
        mLinearLayout = (LinearLayout) findViewById(R.id.linearlayout);

        noteDb = NoteDb.getInstance(this);
        //cursor = noteDb.db.query("note",null,null,null,null,null,null);
        mListView = (ListView) findViewById(R.id.list_view);
        cursor = noteDb.queryAll();
        adapter = new MyAdapter(MainActivity.this,cursor);
        mListView.setAdapter(adapter);

        note_new = (Button) findViewById(R.id.note_new);
        note_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,EditNoteActivity.class);
                startActivityForResult(intent,100);
            }
        });

/*        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //长按Item进入多选模式
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.ismultiMode = true;
                mLinearLayout.setVisibility(View.VISIBLE);
                note_new.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();

                return true;
            }
        });

        //点按Item进行选择
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.ismultiMode){
                    CheckBox cb = (CheckBox) view.findViewById(R.id.check);
                    Boolean isCheck = !cb.isChecked();
                    cb.setChecked(isCheck);
                    if (isCheck){
                        count++;
                    }else {
                        count--;
                    }
                    delete.setText("删除(" + count + ")");
                    selectId = adapter.getItemId(position);
                    recordStatus.put(position,isCheck);
                    recordCursorIdStatus.put(selectId,isCheck);
                }else {
                    Toast.makeText(MainActivity.this, "需要弹出编辑界面" + position, Toast.LENGTH_SHORT).show();
                    Long editId = adapter.getItemId(position);
                    Cursor cursor = adapter.getCursor();
                    cursor.moveToPosition(position);
                    String edittitle = cursor.getString(cursor.getColumnIndex("title"));
                    String editcontent = cursor.getString(cursor.getColumnIndex("content"));
                    String editdate = cursor.getString(cursor.getColumnIndex("date"));
                    Intent intent = new Intent(MainActivity.this,EditNoteActivity.class);
                    intent.putExtra("edittitle",edittitle);
                    intent.putExtra("editcontent",editcontent);
                    intent.putExtra("editdate",editdate);
                    startActivityForResult(intent,101);
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.ismultiMode){
                    cancelAction();
                }
            }
        });

        choseAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (adapter.ismultiMode){
                  adapter.choseAll = true;
                  Cursor cursor = noteDb.queryAll();
                  if (cursor.moveToFirst()){
                      do {
                          int i = 0;
                          Long id = cursor.getLong(cursor.getColumnIndex("_id"));
                          recordStatus.put(0,true);
                          recordCursorIdStatus.put(id,true);
                          i++;
                      }while (cursor.moveToNext());
                  }
                  cursor.close();

                  adapter.notifyDataSetChanged();
                  delete.setText("删除(" + adapter.getCount() + ")");
                  Log.d("count",adapter.getCount()+"");
              }
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (adapter.ismultiMode){
                  for (Map.Entry<Long,Boolean> entry : recordCursorIdStatus.entrySet()){
                      if (entry.getValue()){
                          noteDb.delete(entry.getKey()+"");
                      }
                  }
                  Cursor cursor = noteDb.queryAll();
                  adapter.changeCursor(cursor);
                  adapter.ismultiMode = false;
                  adapter.notifyDataSetChanged();
                  mLinearLayout.setVisibility(View.GONE);
                  note_new.setVisibility(View.VISIBLE);
                  count = 0;
                  recordCursorIdStatus.clear();
                  recordStatus.clear();

              }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        cursor = noteDb.queryAll();
        adapter.changeCursor(cursor);
        switch (requestCode){
            case 100:                  //新增便签返回列表时
                adapter.notifyDataSetChanged();
                break;
            case 101:                  //修改便签返回列表时
                adapter.notifyDataSetChanged();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (adapter.ismultiMode){
          cancelAction();
        }else {
            super.onBackPressed();
        }
    }

    private void cancelAction(){
        mLinearLayout.setVisibility(View.GONE);
        note_new.setVisibility(View.VISIBLE);
        adapter.ismultiMode = false;
        adapter.choseAll = false;
        adapter.notifyDataSetChanged();
        recordCursorIdStatus.clear();
        recordStatus.clear();
        count = 0;
        delete.setText("删除("+count+")");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_all) {
            // Handle the camera action


        } else if (id == R.id.nav_work) {


           /* toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle("工作");
            setSupportActionBar(toolbar);*/
        } else if (id == R.id.nav_life) {

        } else if (id == R.id.nav_other) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_about) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
