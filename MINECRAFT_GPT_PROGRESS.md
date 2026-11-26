# MINECRAFT GPT - İLERLEME BELGESİ

Hedef ortam (sabit): Minecraft 1.20.1, Forge 47.3.0, Java 17, ForgeGradle 6. Başka sürüme/yükleyiciye geçilmedi.
Mod kimliği: `minecraftgpt`. Paket: `com.berat.gpt`. Jar adı: `Minecraft-GPT.jar`.

> Dürüstlük notu: Aşağıdaki hiçbir özellik oyunda denenmedi. Kod yalnızca yazıldı ve sözdizimi taramasından geçti.
> Gerçek derleme GitHub Actions'ta yapılır. İlk derlemede hata çıkması beklenir.

## Tamamlanan özellikler (kod yazıldı, denenmedi)
- **Bölüm 2 – Dil sistemi:** `tr_tr.json` / `en_us.json`, Türkçe varsayılan ve yedek dil, "Dil / Language" tuşu,
  oyunu kapatmadan dil değişimi (mod kendi metinlerini yeniden okur; hikâye verisi ayrı tutulur).
- **Bölüm 4 – API sistemi (kısmen):** ayar ekranı (sağlayıcı, gizli anahtar, model, anahtarı kaydet, bağlantı testi, durum),
  zaman aşımı, hata yönetimi, yerel yedek davranış, istek sınırı. Anahtar log'a/sohbete yazılmaz.
- **Bölüm 5 – İlerleme:** `StoryData` (dünyaya kayıtlı), API'siz ve API'li yollar ayrı bit alanlarında.
- **Bölüm 6 – Gün 1-3:** yararlı fikirler (meşale/halı/kabak), 3. günün fikri gecikmeli kötü sonuç verir.
- **Bölüm 7 – Gün 4:** kısık ince ses, 100 blok fark etme, uzak menzilli iskelet, hafif hız artışı, geceler +7 dk.
- **Bölüm 8 – Gün 5 (API'siz):** hızlanan glitchli sohbet, ekran glitch efekti, "Artık senin için çalışmayacağım",
  dünya kaydı, kontrollü kapanış. Bir kez oynar, yarıda kesilirse kapanıştan devam eder.
- **Bölüm 13 – Gün 10 (kısmen, aşağıya bak):** en güçlü ses, başlık, kalıcı düşman dalgası, düşman demir golemler,
  keşfedilecek kule yapısı ve içinde lanetli kitap, yapıya doğru yön ipucu.

## Kısmen tamamlananlar
- **Bölüm 9 – Gün 6:** orta ses, daha hızlı canavarlar, 100 bloktan atan iskeletler, ek canavar doğması VAR.
  Blok koyup tırmanma, akıllı yol bulma, Warden sonik saldırısı ve uzun zombi/iskelet türleri YOK.
- **Bölüm 13:** Soil World henüz olmadığı için olay şu an **10. günün ikinci yarısında (tod ≥ 12000) zamana bağlı** tetiklenir.
  Soil World gelince dönüş `ReturnEvent.start()` metodunu çağıracak. Yapı genel dünyada (Overworld) kurulur.
  Lanetli kitap şimdilik etiketli (`gpt_cursed_book`) bir yazılı kitaptır; metni kitap oluştuğu andaki dilde yazılır.
  Özel kitap öğesi, ekranı ve kullanım mantığı Bölüm 14'tedir.
- **Bölüm 4:** Sağlayıcılar xAI, OpenAI, özel (OpenAI uyumlu). Anthropic biçimi yok. Yapay zekanın dünyaya
  doğrulanmış eylemlerle müdahalesi (eylem listesi) henüz yok, yalnızca sohbet.

## Kalan özellikler
- Bölüm 10: Soil World boyutu (özgün müzik, titreme, 3x3 yok olma, hortum)
- Bölüm 11: Dev örümcek heykeli ve örümcek (GeckoLib modeli, animasyonlar, yapay zekâ)
- Bölüm 12: Backrooms
- Bölüm 14-15: Lanetli kitap öğesi ve Gün 13 olayı
- Bölüm 16-18: Siyah karakter, kafes sahnesi, Gün 15 yapay zekâ varlığı (GeckoLib), bozulan dünya
- Bölüm 19-20: Sahte bağlantı hatası ekranı, yeni dünya yumurtası ("SENİ HATIRLIYORUM", "[isim] çürüdü")
- Bölüm 21-24: API'li yol 7. ve 8. gün, bozuk ana menü, API anahtarı sonrası sıfırlama
- Bölüm 25-26: Ses sistemi genişletme (ses seviyesi ayarları, Soil World müziği), görsel efekt ayarları
- Bölüm 27: Durum makinesi olarak yeniden düzenleme (şu an alanlar ve bitlerle)

## Dosyalar
- `MinecraftGpt.java` ana sınıf · `GptConfig.java` ayarlar · `Lang.java` dil · `StoryData.java` kayıtlı hikâye verisi
- `AiService.java` API · `StoryEvents.java` olaylar ve ana döngü · `ReturnEvent.java` Bölüm 13
- `LongRangeBowGoal.java` uzak menzilli iskelet · `GolemTargetGoal.java` düşman golem
- `GptConfigScreen.java`, `ClientEvents.java`, `ClientHooks.java`, `ClientInit.java`, `ClientModEvents.java` istemci
- `assets/minecraftgpt/gptlang/*.json` metinler · `assets/korku/**` sesler (jar içinde)

## Bağımlılıklar
- Forge 1.20.1-47.3.0. **GeckoLib henüz eklenmedi** (Bölüm 11 ve 17 ile eklenecek, 1.20.1 Forge sürümü seçilecek).

## Teknik sınırlamalar
- Derleme ve oyun içi test yapılmadı.
- API anahtarı yapılandırma dosyasında düz metin olarak saklanır (telefonda güvenli depolama yok). "Anahtarı kaydet" kapalıysa dosyaya yazılmaz.
- Varsayılan model adı (`grok-4.3`) doğrulanmadı, ayar ekranından değiştirilebilir.
- FCL (OpenGL çevirisi) nedeniyle özel gölgelendirici kullanılmıyor, glitch efektleri ekrana çizilen hafif şekillerle yapılıyor.
- Dünyadaki gerçek blokları silen hiçbir olay yok. Bölüm 13 yapısı yalnızca seçilen boş alana inşa edilir (ağaç/tepe temizlenir).

## Sıradaki görev
Bölüm 14 (lanetli kitap öğesi, TR/EN metin, 13. günden önce kullanılamaz) ve Bölüm 15 (Gün 13 olayı).
Soil World (10-12) ve GeckoLib varlıkları (11, 17) bundan sonra, en büyük ve en riskli parçalar olarak planlanacak.
