package com.kartik.myschool.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<Teacher> teacher = new MutableLiveData<>();
    private final FirebaseRepository repo = FirebaseRepository.get();

    public LiveData<Teacher> getTeacher() { return teacher; }

    public void loadTeacher() {
        repo.getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) { teacher.postValue(t); }
            @Override public void onError(Exception e) { teacher.postValue(null); }
        });
    }
}
