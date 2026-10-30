package pe.upeu.resi.resiup

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform