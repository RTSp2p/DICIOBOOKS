sed -i '/val frameLayout = FrameLayout(this)/a \
\
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(frameLayout) { view, insets ->\
            val imeHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom\
            view.setPadding(0, 0, 0, imeHeight)\
            insets\
        }' app/src/main/java/com/example/service/FloatingDictionaryService.kt
