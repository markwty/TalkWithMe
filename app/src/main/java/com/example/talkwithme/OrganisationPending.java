package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class OrganisationPending extends Fragment {
    private String classname = getClass().getName();
    private int id = -1, opp_id = -1, position;
    private LinkedList<Contact> fullContactsList = new LinkedList<>();
    private LinkedList<Contact> contactsList = new LinkedList<>();
    private ContactsRecyclerAdapter contactsRecyclerAdapter;
    private SearchView SearchView;
    private RecyclerView ContactsRecyclerView;
    private SwipeRefreshLayout ContactsRefreshLayout;
    private ScrollView FormScrollView;
    private ImageView FormImageView;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.organisation_pending, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                String mobile = prefs.getString("mobile", "");
                String password = prefs.getString("password", "");
                //In case of using account on another phone, will update accordingly as preferences is unique to each phone
                String modified = prefs.getString("modified" + id, "0000-00-00 00:00:00");
                DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "pending_table");
                Cursor res = dbHelper.getAllData();
                StringBuilder local_list_str = new StringBuilder();
                boolean first = true;
                if(res.getCount() > 0) {
                    while (res.moveToNext()) {
                        if(first){
                            local_list_str.append(res.getInt(res.getColumnIndex("ID")));
                            first = false;
                        }else{
                            local_list_str.append(",").append(res.getInt(res.getColumnIndex("ID")));
                        }
                    }
                }
                res.close();
                dbHelper.db.close();
                RunnableTask task = new UpdatePendingRunnableTask(context, StartActivity.host_dir + "update_pending.php"
                        , new String[]{"mobile", "password", "local_list_str", "modified"}, new String[]{mobile, password, local_list_str.toString(), modified}
                        , "pending_table", id);
                Thread thread = new Thread(task);
                thread.start();
            }
        });
        thread.start();

        //RecyclerView is used as it is efficient, R.layout.contact is the layout of each entry
        contactsRecyclerAdapter = new ContactsRecyclerAdapter(R.layout.contact, contactsList);
        ContactsRecyclerView = view.findViewById(R.id.ContactsRecyclerView);
        ContactsRecyclerView.setAdapter(contactsRecyclerAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        ContactsRecyclerView.setLayoutManager(layoutManager);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //itemDecoration are transparent blocks to customise spacing between entries
                //200 interspace to show demo for scroll, original = 30
                final CustomItemDecoration itemDecoration = new CustomItemDecoration(0, 30, 30);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Having a fixed size provides a performance boost i.e. faster calculation/preparation of views
                        ContactsRecyclerView.addItemDecoration(itemDecoration);
                        ContactsRecyclerView.setHasFixedSize(true);
                    }
                });
                //Color of refresh icon as it spins, may not see all depending on how long it spins
                ContactsRefreshLayout = view.findViewById(R.id.ContactsRefreshLayout);
                ContactsRefreshLayout.setColorSchemeResources(
                        android.R.color.holo_blue_light,
                        android.R.color.holo_orange_light,
                        android.R.color.holo_green_light,
                        android.R.color.holo_red_light);
                ContactsRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refresh();
                    }
                });

                SearchView = view.findViewById(R.id.SearchView);
                SearchView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Prepares for users to type in search query
                        SearchView.setIconified(false);
                    }
                });
                SearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    //Filter contacts
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        contactsRecyclerAdapter.filter(newText);
                        return false;
                    }
                });

                FormScrollView = view.findViewById(R.id.FormScrollView);
                FormImageView = view.findViewById(R.id.FormImageView);
                final Button BackButton = view.findViewById(R.id.BackButton);
                BackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SearchView.setVisibility(View.VISIBLE);
                                ContactsRecyclerView.setVisibility(View.VISIBLE);
                                FormScrollView.setVisibility(View.GONE);
                            }
                        });
                    }
                });
                final Button AcceptButton = view.findViewById(R.id.AcceptButton);
                AcceptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                        RunnableTask task = new AcceptApplicationRunnableTask(context, StartActivity.host_dir + "accept_application.php"
                                , new String[]{"mobile", "password", "opp_id"},
                                new String[]{prefs.getString("mobile", ""), prefs.getString("password", ""), Integer.toString(opp_id)}, position);
                        Thread thread = new Thread(task);
                        thread.start();
                    }
                });
                final Button RejectButton = view.findViewById(R.id.RejectButton);
                RejectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                        RunnableTask task = new RejectApplicationRunnableTask(context, StartActivity.host_dir + "reject_application.php"
                                , new String[]{"mobile", "password", "opp_id"},
                                new String[]{prefs.getString("mobile", ""), prefs.getString("password", ""), Integer.toString(opp_id)}, position);
                        Thread thread = new Thread(task);
                        thread.start();
                    }
                });
            }
        });
        thread.start();
        return view;
    }

    private class AcceptApplicationRunnableTask extends RunnableTask{
        private int position;
        AcceptApplicationRunnableTask(Context context, String address, String[] categories, String[] args, int position){
            super(context, address, categories, args);
            this.position = position;
        }
        public void run() {
            super.run();
            if(result.equals("")){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //If there is an error, RunnableTask instance will set the result to ""(empty string)
                        //This is assumed to be a connection error
                        Toast.makeText(context, "Check your connection", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            try {
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Success")) {
                    DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "pending_table");
                    //Removes entry from existing view
                    String sopp_id = find("opp_id");
                    if(sopp_id == null){
                        return;
                    }
                    int opp_id = Integer.parseInt(sopp_id);
                    dbHelper.deleteData(sopp_id);
                    contactsList.remove(position);
                    int index = 0;
                    for (Contact contact : fullContactsList) {
                        if (contact.id == opp_id) {
                            break;
                        }
                        index += 1;
                    }
                    fullContactsList.remove(index);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Informs the view to update according to contactsList
                            contactsRecyclerAdapter.notifyDataSetChanged();
                            SearchView.setVisibility(View.VISIBLE);
                            ContactsRecyclerView.setVisibility(View.VISIBLE);
                            FormScrollView.setVisibility(View.GONE);
                        }
                    });
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }

    private class RejectApplicationRunnableTask extends RunnableTask{
        int position;
        RejectApplicationRunnableTask(Context context, String address, String[] categories, String[] args, int position){
            super(context, address, categories, args);
            this.position = position;
        }
        public void run() {
            super.run();
            if(result.equals("")){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //If there is an error, RunnableTask instance will set the result to ""(empty string)
                        //This is assumed to be a connection error
                        Toast.makeText(context, "Check your connection", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            try {
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Success")) {
                    String sopp_id = find("opp_id");
                    if(sopp_id == null){
                        return;
                    }
                    int opp_id = Integer.parseInt(sopp_id);
                    DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "pending_table");
                    dbHelper.deleteData(sopp_id);
                    /*
                    //Image resources may be shared between profiles (in the cache)
                    boolean success = new ImageUtils().deleteBitmap(context, sopp_id);
                    if(!success){
                        Log.v(classname, "Image not deleted");
                    }
                     */
                    contactsList.remove(position);
                    int index = 0;
                    for (Contact contact : fullContactsList) {
                        if (contact.id == opp_id) {
                            break;
                        }
                        index += 1;
                    }
                    fullContactsList.remove(index);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            contactsRecyclerAdapter.notifyDataSetChanged();
                            SearchView.setVisibility(View.VISIBLE);
                            ContactsRecyclerView.setVisibility(View.VISIBLE);
                            FormScrollView.setVisibility(View.GONE);
                        }
                    });
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                String mobile = prefs.getString("mobile", "");
                String password = prefs.getString("password", "");
                //In case of using account on another phone, will update accordingly as preferences is unique to each phone
                String modified = prefs.getString("modified" + id, "0000-00-00 00:00:00");
                DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "pending_table");
                Cursor res = dbHelper.getAllData();
                StringBuilder local_list_str = new StringBuilder();
                boolean first = true;
                if(res.getCount() > 0) {
                    while (res.moveToNext()) {
                        if(first){
                            local_list_str.append(res.getInt(res.getColumnIndex("ID")));
                            first = false;
                        }else{
                            local_list_str.append(",").append(res.getInt(res.getColumnIndex("ID")));
                        }
                    }
                }
                res.close();
                dbHelper.db.close();
                RunnableTask task = new UpdatePendingRunnableTask(context, StartActivity.host_dir + "update_pending.php"
                        , new String[]{"mobile", "password", "local_list_str", "modified"}, new String[]{mobile, password, local_list_str.toString(), modified}
                        , "pending_table", id);
                Thread thread = new Thread(task);
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Log.e(classname, e.toString());
                } finally {
                    //Stops and remove the refreshing icon when the "refresh" process is done
                    if (ContactsRefreshLayout.isRefreshing()) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ContactsRefreshLayout.setRefreshing(false);
                            }
                        });
                    }
                }
            }
        });
        thread.start();
    }

    private class UpdatePendingRunnableTask extends UpdateContactsRunnableTask {
        UpdatePendingRunnableTask(Context context, String address, String[] categories, String[] args
                , String TABLE_NAME, int id) {
            super(context, address, categories, args, TABLE_NAME, id);
        }
        public void run() {
            super.run();
            updateAdapter();
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAdapter() {
        //Getting all data from table (local database) and update list of contacts
        DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "pending_table");
        Cursor res = dbHelper.getAllData();
        contactsList.clear();
        if (res.getCount() == 0) {
            Log.v(classname, "Nothing to display");
        }else{
            contactsList.clear();
            while (res.moveToNext()) {
                String name, gender;
                int id = res.getInt(res.getColumnIndex("ID"));
                name = res.getString(res.getColumnIndex("Name"));
                //Gender needs to be converted to a user-friendly/readable form
                if (res.getString(res.getColumnIndex("Gender")).equals("0")) {
                    gender = "Male";
                } else {
                    gender = "Female";
                }
                Contact contact = new Contact(id, name, gender);
                contactsList.add(contact);
            }
            //Contacts are ordered by name (in alphabetical order)
            Collections.sort(contactsList, new Comparator<Contact>() {
                @Override
                public int compare(Contact c1, Contact c2) {
                    return c1.name.compareTo(c2.name);
                }
            });
        }
        //fullContactsList is a full list needed as a reference to filter from
        fullContactsList = (LinkedList<Contact>)contactsList.clone();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Informs the view to update according to contactsList
                contactsRecyclerAdapter.notifyDataSetChanged();
            }
        });
        res.close();
        dbHelper.db.close();
    }

    static class Contact{
        int id;
        String name, gender;
        Contact(int id, String name, String gender) {
            this.id = id;
            this.name = name;
            this.gender = gender;
        }
    }

    class ContactsRecyclerAdapter extends RecyclerView.Adapter<ContactsRecyclerAdapter.ViewHolder> {
        private int listItemLayout;
        private LinkedList<Contact> itemList;

        ContactsRecyclerAdapter(int layoutId, LinkedList<Contact> items) {
            listItemLayout = layoutId;
            itemList = items;
        }

        @Override
        public int getItemCount() {
            return itemList == null ? 0 : itemList.size();
        }

        @Override
        @NonNull
        public ContactsRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(listItemLayout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ContactsRecyclerAdapter.ViewHolder holder, final int position) {
            final View convertView = holder.convertView;
            //Filling up the R.layout.contact layout
            ImageView ProfileImageView = convertView.findViewById(R.id.ProfileImageView);
            TextView InfoRow = convertView.findViewById(R.id.InfoRow);
            TextView StatusRow = convertView.findViewById(R.id.StatusRow);
            Contact contact = itemList.get(position);
            new ImageUtils().setImage(context, ProfileImageView, Integer.toString(contact.id), contact.gender, ImageUtils.defaultIcon);
            InfoRow.setText(context.getResources().getString(R.string.Info2, contact.name, contact.gender));
            StatusRow.setText(R.string.Empty);
            //Associating id to the view, so it can be reference later i.e. add friend on click of contact
            convertView.setTag(contact.id);
        }

        void filter(String newText) {
            newText = newText.toLowerCase();
            contactsList.clear();
            if (newText.length() == 0) {
                contactsList.addAll(fullContactsList);
            } else {
                for (Contact contact: fullContactsList) {
                    if (contact.name.toLowerCase().contains(newText)) {
                        contactsList.add(contact);
                    }
                }
            }
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            View convertView;

            ViewHolder(View convertView) {
                super(convertView);
                this.convertView = convertView;
                convertView.setOnClickListener(this);
            }

            @Override
            public void onClick(final View view) {
                position = this.getAdapterPosition();
                //Get back the associated id here
                opp_id = Integer.parseInt(view.getTag().toString());
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                RunnableTask task = new GetFormRunnableTask(context, StartActivity.host_dir + "get_form.php"
                        , new String[]{"opp_id", "mobile", "password"},
                        new String[]{Integer.toString(opp_id), prefs.getString("mobile", ""), prefs.getString("password", "")});
                Thread thread = new Thread(task);
                thread.start();
            }
        }
    }

    private class GetFormRunnableTask extends RunnableTask {
        GetFormRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }

        public void run() {
            super.run();
            if(result.equals("")){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //If there is an error, RunnableTask instance will set the result to ""(empty string)
                        //This is assumed to be a connection error
                        Toast.makeText(context, "Check your connection", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            try {
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Success")) {
                    String encodedImage = collection.getString("Image");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SearchView.setVisibility(View.GONE);
                            ContactsRecyclerView.setVisibility(View.GONE);
                            FormScrollView.setVisibility(View.VISIBLE);
                            new ImageUtils().setImage(activity, FormImageView, encodedImage, ImageUtils.defaultBackground);
                        }
                    });
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }
}