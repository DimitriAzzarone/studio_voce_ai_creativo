# Studio Voce AI

Applicazione Android nativa realizzata con Kotlin, Jetpack Compose e Material 3.

## Funzioni reali presenti

- interfaccia scura con identità visiva di Studio Voce AI;
- selezione audio tramite `ActivityResultContracts.OpenDocument`;
- URI del Storage Access Framework senza conversione in percorsi filesystem;
- richiesta del permesso persistente quando il provider lo consente;
- nome, MIME e dimensione del file;
- rimozione della selezione;
- riproduzione audio reale con Media3 ExoPlayer;
- Play, Pausa, ripresa, Stop e seek;
- posizione corrente e durata totale;
- aggiornamento periodico della posizione;
- waveform ricavata realmente dal file tramite `MediaExtractor` e `MediaCodec`, quando il codec è decodificabile;
- evidenziazione della parte già riprodotta;
- rilascio di ExoPlayer e del thread di analisi alla distruzione della schermata;
- modelli Kotlin preliminari per una futura partitura;
- `MusicXmlExporter` ancora controllato come `NotImplemented`.

## Comportamento della fase 2

Quando viene scelto un file, l'app prepara un solo ExoPlayer usando `MediaItem.fromUri(uri)`. La selezione di un secondo file sostituisce il contenuto precedente. Play, pausa, ripresa, seek e stop comandano realmente il player; Stop riporta la posizione a `00:00`.

La waveform non contiene valori casuali: viene decodificata fuori dal thread UI in PCM e ridotta a un numero limitato di barre normalizzate. Se Android non riesce a decodificare il formato, la riproduzione può restare disponibile e l'interfaccia mostra il limite senza simulare un'onda.

URI e metadati sono mantenuti tramite `rememberSaveable` durante la rotazione. Il player non viene mantenuto attraverso una ricreazione completa dell'Activity: viene rilasciato e il file selezionato viene preparato nuovamente.

## Funzioni non implementate

- separazione AI di voce e accompagnamento;
- isolamento della voce;
- trascrizione Whisper;
- riconoscimento di melodia o accordi;
- generazione reale o professionale della partitura;
- esportazione MusicXML effettiva;
- testo sincronizzato;
- karaoke;
- esportazioni audio o grafiche.

I pannelli futuri restano inattivi e mostrano esclusivamente stati reali.

## Limiti reali

- la waveform non è garantita per ogni codec o provider;
- durata e waveform possono non essere disponibili per file danneggiati, vuoti o con metadati incompleti;
- prestazioni e tempo di analisi dipendono da durata, formato e potenza del dispositivo;
- il permesso persistente dipende dal provider scelto;
- non sono richiesti permessi di archiviazione, registrazione audio o accesso a Internet;
- non vengono usati server locali, Python sul telefono, chiavi API o modelli AI nell'APK.

## Tecnologie

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Gradle 8.9 nel workflow GitHub Actions
- Java 17
- Jetpack Compose e Material 3
- Media3 ExoPlayer 1.10.1
- `MediaExtractor` e `MediaCodec`
- compileSdk 35
- targetSdk 35
- minSdk 26

## Gradle Wrapper

Il repository non include ancora il Gradle Wrapper completo. GitHub Actions installa Gradle 8.9 tramite `gradle/actions/setup-gradle`.

## Compilazione e Artifact

La verifica principale avviene tramite GitHub Actions:

```text
gradle projects --stacktrace
gradle assembleDebug --stacktrace
```

Percorso dell'APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Artifact:

```text
Studio-Voce-AI-debug-<nome-ramo>
```

La build è confermata soltanto quando GitHub Actions è verde e l'APK è realmente disponibile.

## Test della fase 2

1. Selezionare un MP3 e verificare nome, MIME e dimensione.
2. Premere Play e verificare l'avanzamento.
3. Mettere in pausa e riprendere.
4. Spostare lo Slider.
5. Premere Stop e verificare il ritorno a `00:00`.
6. Verificare la waveform.
7. Selezionare un secondo file e controllare la sostituzione del precedente.
8. Provare AAC/M4A e WAV.
9. Provare un file non supportato.
10. Ruotare lo schermo, chiudere e riaprire l'app e verificare l'assenza di crash.

## Roadmap

1. Confermare la fase 2 con GitHub Actions e prove su dispositivo.
2. Valutare un modello audio specifico ottimizzato per Android per separare voce e accompagnamento.
3. Aggiungere ascolto e salvataggio delle tracce realmente elaborate.
4. Valutare un modello tipo Whisper.
5. Implementare analisi melodica e ritmica reale.
6. Generare MusicXML, immagini e PDF solo da risultati musicali reali.
7. Aggiungere progressivamente testo sincronizzato e karaoke.
