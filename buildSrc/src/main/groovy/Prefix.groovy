enum Prefix {
    HDR ('.h'),
    CPP ('.cpp'),
    JAV ('.java'),
    PYC ('.py'),
    ICE ('.ice')

    Prefix(String fileExtension) {
        this.extension = fileExtension
    }

    final String extension
}