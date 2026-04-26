package com.vibenote.app.presentation;

import com.vibenote.app.domain.repository.NoteRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<NoteRepository> noteRepositoryProvider;

  public MainActivity_MembersInjector(Provider<NoteRepository> noteRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<NoteRepository> noteRepositoryProvider) {
    return new MainActivity_MembersInjector(noteRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectNoteRepository(instance, noteRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.vibenote.app.presentation.MainActivity.noteRepository")
  public static void injectNoteRepository(MainActivity instance, NoteRepository noteRepository) {
    instance.noteRepository = noteRepository;
  }
}
