package com.vibenote.app.di;

import android.content.Context;
import com.vibenote.app.data.local.NoteDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DatabaseModule_ProvideNoteDatabaseFactory implements Factory<NoteDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideNoteDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NoteDatabase get() {
    return provideNoteDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideNoteDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideNoteDatabaseFactory(contextProvider);
  }

  public static NoteDatabase provideNoteDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideNoteDatabase(context));
  }
}
