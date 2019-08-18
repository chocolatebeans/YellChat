class DepConfig {

    boolean isUseLocal  //是否使用本地
    String localPath    //本地路径
    String remotePath   //远程路径
    boolean isApply     //是否应用
    String path         //最后的路径
    def dep             //根据条件生成项目最终的依赖项

    DepConfig(String path) {
        this(path, true)
    }

    DepConfig(String path, boolean isApply) {
        if (path.startsWith(":")) {
            this.isUseLocal = true
            this.localPath = path
            this.isApply = isApply
        } else {
            this.isUseLocal = false
            this.remotePath = path
            this.isApply = isApply
        }
        this.path = path
    }

    DepConfig(boolean isUseLocal, String localPath, String remotePath) {
        this(isUseLocal, localPath, remotePath, true)
    }

    DepConfig(boolean isUseLocal, String localPath, String remotePath, boolean isApply) {
        this.isUseLocal = isUseLocal
        this.localPath = localPath
        this.remotePath = remotePath
        this.isApply = isApply
        this.path = isUseLocal ? localPath : remotePath
    }

    @Override
    public String toString() {
        return "DepConfig{" +
                "isUseLocal=" + isUseLocal +
                ", localPath='" + localPath + '\'' +
                ", remotePath='" + remotePath + '\'' +
                ", isApply=" + isApply +
                ", path='" + path + '\'' +
                ", dep=" + dep +
                '}'
    }
}