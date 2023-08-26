package com.example.talkwithme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Contacts extends Fragment {
    private static int id = -1, gender = -1, status = -1, state = -1;
    private static String name = "", organisation = "";
    private String[] tabNames = new String[]{"Particulars", "Current", "Pending"};
    private ViewPager2 ContactsViewPager;
    CollectionAdapter collectionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            organisation = bundle.getString("organisation", "");
            status = bundle.getInt("status", -1);
            state = bundle.getInt("state", -1);
        }
        return inflater.inflate(R.layout.contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TabLayout ContactsTabLayout = view.findViewById(R.id.ContactsTabLayout);
        //Setting names for the tabs
        for(String tabName:tabNames){
            ContactsTabLayout.addTab(ContactsTabLayout.newTab().setText(tabName));
        }
        ContactsTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        collectionAdapter = new CollectionAdapter(this, ContactsTabLayout.getTabCount());
        ContactsViewPager = view.findViewById(R.id.ContactsViewPager);
        ContactsViewPager.setAdapter(collectionAdapter);

        //TabLayoutMediator synchronises the viewpager and tab layout
        new TabLayoutMediator(ContactsTabLayout, ContactsViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(tabNames[position]);
                ContactsViewPager.setCurrentItem(position, true);
            }
        }).attach();
        ContactsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //Change tab on click of the names of the tabs
                ContactsViewPager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //Set default tab
        ContactsViewPager.setCurrentItem(0);
    }

    static class CollectionAdapter extends FragmentStateAdapter {
        private int totalTabs;
        Fragment[] fragments = {null, null, null};

        CollectionAdapter(Fragment fragment, int totalTabs) {
            super(fragment);
            this.totalTabs = totalTabs;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            //Zero-based index
            if(position == 0){
                fragment = new Particulars();
                Bundle bundle = new Bundle();
                bundle.putInt("id", id);
                bundle.putString("name", name);
                bundle.putInt("gender", gender);
                bundle.putString("organisation", organisation);
                bundle.putInt("status", status);
                bundle.putInt("state", state);
                fragment.setArguments(bundle);
            }else if(position == 1){
                if(status == 2){
                    fragment = new OrganisationCurrent();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    fragment.setArguments(bundle);
                }else{
                    fragment = new ContactsCurrent();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    fragment.setArguments(bundle);
                }
            }else{
                //Should only be 3 but just in case there are more to prevent application from crashing
                if(status == 2){
                    fragment = new OrganisationPending();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    fragment.setArguments(bundle);
                }else{
                    fragment = new ContactsPending();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    fragment.setArguments(bundle);
                }
            }

            //Extra safety measure to ensure the app does not crash
            if(position < 3){
                fragments[position] = fragment;
            }
            return fragment;
        }

        @Override
        public int getItemCount() {
            return totalTabs;
        }

    }
}
