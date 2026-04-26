package com.vibenote.app.presentation.canvas;

import android.content.Context;
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
public final class CanvasViewModel_Factory implements Factory<CanvasViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<NoteRepository> noteRepositoryProvider;

  public CanvasViewModel_Factory(Provider<Context> contextProvider,
      Provider<NoteRepository> noteRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.noteRepositoryProvider = noteRepositoryProvider;
  }

  @Override
  public CanvasViewModel get() {
    return newInstance(contextProvider.get(), noteRepositoryProvider.get());
  }

  public static CanvasViewModel_Factory create(Provider<Context> contextProvider,
      Provider<NoteRepository> noteRepositoryProvider) {
    return new CanvasViewModel_Factory(contextProvider, noteRepositoryProvider);
  }

  public static CanvasViewModel newInstance(Context context, NoteRepository noteRepository) {
    return new CanvasViewModel(context, noteRepository);
  }
}
