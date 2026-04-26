package com.vibenote.app.presentation.dashboard;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.vibenote.app.domain.repository.NoteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<NoteRepository> noteRepositoryProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public DashboardViewModel_Factory(Provider<Context> contextProvider,
      Provider<NoteRepository> noteRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.contextProvider = contextProvider;
    this.noteRepositoryProvider = noteRepositoryProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(contextProvider.get(), noteRepositoryProvider.get(), dataStoreProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<Context> contextProvider,
      Provider<NoteRepository> noteRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new DashboardViewModel_Factory(contextProvider, noteRepositoryProvider, dataStoreProvider);
  }

  public static DashboardViewModel newInstance(Context context, NoteRepository noteRepository,
      DataStore<Preferences> dataStore) {
    return new DashboardViewModel(context, noteRepository, dataStore);
  }
}
