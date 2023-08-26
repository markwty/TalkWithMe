package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class SelectOrganisation extends Fragment {
    private String classname = getClass().getName();
    private int id = -1, gender = -1, state = -1;
    private String name = "";
    private LinkedList<Organisation> fullOrganisationList = new LinkedList<>();
    private LinkedList<Organisation> organisationList = new LinkedList<>();
    private OrganisationRecyclerAdapter organisationRecyclerAdapter;
    private RecyclerView OrganisationRecyclerView;
    private SwipeRefreshLayout OrganisationRefreshLayout;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.select_organisation, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            state = bundle.getInt("state", -1);
        }

        RunnableTask task = new GetOrganisationsRunnableTask(context, StartActivity.host_dir + "get_all_organisations.php"
                , new String[]{}, new String[]{});
        Thread thread = new Thread(task);
        thread.start();

        //RecyclerView is used as it is efficient, R.layout.organisation is the layout of each entry
        organisationRecyclerAdapter = new OrganisationRecyclerAdapter(R.layout.organisation, organisationList);
        OrganisationRecyclerView = view.findViewById(R.id.OrganisationRecyclerView);
        OrganisationRecyclerView.setAdapter(organisationRecyclerAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        OrganisationRecyclerView.setLayoutManager(layoutManager);
        thread = new Thread(new Runnable(){
            @Override
            public void run(){
                //itemDecoration are transparent blocks to customise spacing between entries
                //200 interspace to show demo for scroll, original = 30
                final CustomItemDecoration itemDecoration = new CustomItemDecoration(0, 30, 30);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OrganisationRecyclerView.addItemDecoration(itemDecoration);
                        //Having a fixed size provides a performance boost i.e. faster calculation/preparation of views
                        OrganisationRecyclerView.setHasFixedSize(true);
                    }
                });

                OrganisationRefreshLayout = view.findViewById(R.id.OrganisationRefreshLayout);
                //Color of refresh icon as it spins, may not see all depending on how long it spins
                OrganisationRefreshLayout.setColorSchemeResources(
                        android.R.color.holo_blue_light,
                        android.R.color.holo_orange_light,
                        android.R.color.holo_green_light,
                        android.R.color.holo_red_light);
                OrganisationRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refresh();
                    }
                });
                final SearchView SearchView = view.findViewById(R.id.SearchView);
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
                        organisationRecyclerAdapter.filter(newText);
                        return false;
                    }
                });
            }
        });
        thread.start();
        return view;
    }

    class GetOrganisationsRunnableTask extends RunnableTask {
        GetOrganisationsRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }

        @SuppressWarnings("unchecked")
        public void run() {
            super.run();
            organisationList.clear();
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
                    collection = collection.getJSONObject("List");
                    for (int i = 0; i < Integer.parseInt(collection.getString("length")); i++) {
                        JSONObject item = collection.getJSONObject(Integer.toString(i));
                        String organisationName = item.getString("Organisation");
                        int form = item.getInt("Form");
                        Organisation organisation = new Organisation(organisationName, form);
                        organisationList.add(organisation);
                    }
                    //Organisations are ordered by name (in alphabetical order)
                    Collections.sort(organisationList, new Comparator<Organisation>() {
                        @Override
                        public int compare(Organisation o1, Organisation o2) {
                            return o1.name.compareTo(o2.name);
                        }
                    });
                    //fullOrganisationList is a full list needed as a reference to filter from
                    fullOrganisationList = (LinkedList<Organisation>)organisationList.clone();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Informs the view to update according to organisationList
                            organisationRecyclerAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    private void refresh(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                RunnableTask task = new GetOrganisationsRunnableTask(context, StartActivity.host_dir + "get_all_organisations.php"
                        , new String[]{}, new String[]{});
                Thread thread = new Thread(task);
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Log.e(classname, e.toString());
                } finally {
                    //Stops and remove the refreshing icon when the "refresh" process is done
                    if (OrganisationRefreshLayout.isRefreshing()) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                OrganisationRefreshLayout.setRefreshing(false);
                            }
                        });
                    }
                }
            }
        });
        thread.start();
    }

    static class Organisation{
        String name;
        int form;
        Organisation(String name, int form) {
            this.name = name;
            this.form = form;
        }
    }

    class OrganisationRecyclerAdapter extends RecyclerView.Adapter<OrganisationRecyclerAdapter.ViewHolder> {
        private int listItemLayout;
        private LinkedList<Organisation> itemList;

        OrganisationRecyclerAdapter(int layoutId, LinkedList<Organisation> items) {
            listItemLayout = layoutId;
            itemList = items;
        }

        @Override
        public int getItemCount() {
            return itemList == null ? 0 : itemList.size();
        }

        @Override
        @NonNull
        public OrganisationRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(listItemLayout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final OrganisationRecyclerAdapter.ViewHolder holder, final int position) {
            final View convertView = holder.convertView;
            TextView NameRow = convertView.findViewById(R.id.NameRow);
            Organisation organisation = itemList.get(position);
            NameRow.setText(organisation.name);
            convertView.setTag(Integer.toString(organisation.form));
        }

        void filter(String newText) {
            newText = newText.toLowerCase();
            organisationList.clear();
            if (newText.length() == 0) {
                organisationList.addAll(fullOrganisationList);
            } else {
                for (Organisation organisation: fullOrganisationList) {
                    if (organisation.name.toLowerCase().contains(newText)) {
                        organisationList.add(organisation);
                    }
                }
            }
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            View convertView;

            ViewHolder(View convertView) {
                super(convertView);
                this.convertView = convertView;
                convertView.setOnClickListener(this);
            }

            @Override
            public void onClick(final View view) {
                TextView NameRow = convertView.findViewById(R.id.NameRow);
                String tag = convertView.getTag().toString();
                Fragment fragment = new FillUpForm();
                Bundle bundle = new Bundle();
                bundle.putInt("id", id);
                bundle.putString("name", name);
                bundle.putInt("gender", gender);
                bundle.putInt("state", state);
                bundle.putString("selected organisation", NameRow.getText().toString());
                bundle.putInt("form", Integer.parseInt(tag));
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        }
    }
}