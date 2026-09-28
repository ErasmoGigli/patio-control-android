# O app não usa reflexão própria. Room e Compose já trazem as suas regras
# via consumer-rules dos artefatos, então não há nada a preservar aqui além
# das linhas de depuração, que ajudam a ler um stack trace de release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
