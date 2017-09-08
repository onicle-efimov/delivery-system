package uz.delivery_system.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation = Paths.get(new StorageProperties().getLocation());

    @Override
    public String store(MultipartFile file) {
        if(file == null){
            return "no_image.png";
        }
        String filename = getAndValidateFilename(file);
        try {
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new StorageException("Fayllardan birini yuklashda xatolik bo'ldi! ", e);
        }
    }

    @Override
    public List<String> storeAll(MultipartFile[] files) {
        List<String> names = new ArrayList<>();
        if(files == null){
            names.add("no_image.png");
            return names;
        }
        try {
            for (int i=0; i<files.length; i++){
                String filename = getAndValidateFilename(files[i]);
                Files.copy(files[i].getInputStream(), this.rootLocation.resolve(filename),
                        StandardCopyOption.REPLACE_EXISTING);
                names.add(filename);
            }
        }
        catch (IOException e) {
            throw new StorageException("Fayllardan birini yuklashda xatolik bo'ldi! ", e);
        }
        return names;
    }

    private String getAndValidateFilename(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Faylni yuklashdaa xatolik");
        }
        String name = file.getOriginalFilename();
        int ind = -1;
        for (int i=name.length()-1; i > 0; i--){
            if (name.charAt(i) =='.'){
                ind = i;
                break;
            }
        }
        if(ind < 0){
            throw new StorageException("Faylni yuklashdaa xatolik");
        }
        return UUID.randomUUID().toString()+name.substring(ind);
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(path -> this.rootLocation.relativize(path));
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            // todo: findFileNameById();
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            System.out.println("Location : "+rootLocation.toAbsolutePath().toString());
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Bean
    public StorageProperties createStorageProperites(){
        return new StorageProperties();
    }
}
