# DroidFPS - Derleme Rehberi

Bu proje senin gönderdigin tüm `.java` kaynak dosyaları ve eksik olan yapılandırma/giriş dosyaları birleştirilerek hazırlanmıştır.

## Derleme (.jar Oluşturma) Adımları

1. Bilgisayarında **JDK 17** kurulu olduğundan emin ol.
2. Bu klasörün içinde bir terminal / komut istemi aç.
3. Eğer klasörde Gradle Wrapper (`gradlew`) yoksa sisteminde kurulu Gradle ile şu komutu çalıştır:
   ```bash
   gradle wrapper --gradle-version 8.8
   ```
4. Ardından modu derlemek için çalıştır:
   - **Windows:** `gradlew build`
   - **Linux / Mac / Termux:** `./gradlew build`
5. İşlem tamamlandığında `build/libs/droidfps-1.0.0.jar` dosyasını alıp Minecraft `mods` klasörüne atabilirsin.
