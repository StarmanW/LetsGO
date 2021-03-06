package com.tarcrsd.letsgo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.tarcrsd.letsgo.Adapters.EventAdapter;
import com.tarcrsd.letsgo.Models.EventAttendees;
import com.tarcrsd.letsgo.Models.Events;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class AttendingFragment extends Fragment {

    // Firebase references
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private RecyclerView mRecycleView;
    private ArrayList<Events> mEventsData;
    private EventAdapter mAdapter;

    public AttendingFragment() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_attending, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initEventRecycleView();
        getEventAttendees();
    }

    private void initEventRecycleView() {
        mRecycleView = getView().findViewById(R.id.recycleViewAttending);
        mRecycleView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        mEventsData = new ArrayList<>();
        mAdapter = new EventAdapter(getActivity(), mEventsData);
        mRecycleView.setAdapter(mAdapter);
    }

    private void getEventAttendees() {
        db.collection("eventAttendees")
                .whereEqualTo("status", 1)
                .whereEqualTo("userUID", db.document("/users/" + mAuth.getUid()))
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                        if (value != null) {
                            getAttendingEvents(value);
                        }
                    }
                });
    }

    private void getAttendingEvents(QuerySnapshot value) {
        for (QueryDocumentSnapshot document : value) {
            mEventsData.clear();
            document.toObject(EventAttendees.class)
                    .getEventID()
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> value) {
                            if (value.isSuccessful()) {
                                mEventsData.add(value.getResult().toObject(Events.class));
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }
}
