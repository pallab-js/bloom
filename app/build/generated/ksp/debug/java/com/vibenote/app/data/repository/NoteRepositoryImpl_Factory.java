package com.vibenote.app.data.repository;

import com.vibenote.app.data.local.NoteDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NoteRepositoryImpl_Factory implements Factory<NoteRepositoryImpl> {
  private final Provider<NoteDao> noteDaoProvider;

  public NoteRepositoryImpl_Factory(Provider<NoteDao> noteDaoProvider) {
    this.noteDaoProvider = noteDaoProvider;
  }

  @Override
  public NoteRepositoryImpl get() {
    return newInstance(noteDaoProvider.get());
  }

  public static NoteRepositoryImpl_Factory create(Provider<NoteDao> noteDaoProvider) {
    return new NoteRepositoryImpl_Factory(noteDaoProvider);
  }

  public static NoteRepositoryImpl newInstance(NoteDao noteDao) {
    return new NoteRepositoryImpl(noteDao);
  }
}
