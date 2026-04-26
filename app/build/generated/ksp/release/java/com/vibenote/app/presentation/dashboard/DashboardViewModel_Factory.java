package com.vibenote.app.presentation.dashboard;

import com.vibenote.app.domain.repository.NoteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
  private final Provider<NoteRepository> noteRepositoryProvider;

  public DashboardViewModel_Factory(Provider<NoteRepository> noteRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(noteRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<NoteRepository> noteRepositoryProvider) {
    return new DashboardViewModel_Factory(noteRepositoryProvider);
  }

  public static DashboardViewModel newInstance(NoteRepository noteRepository) {
    return new DashboardViewModel(noteRepository);
  }
}
