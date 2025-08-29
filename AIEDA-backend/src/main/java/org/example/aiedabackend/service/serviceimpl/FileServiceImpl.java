package org.example.aiedabackend.service.serviceimpl;

import org.example.aiedabackend.dao.FileRepository;
import org.example.aiedabackend.po.File;
import org.example.aiedabackend.service.FileService;
import org.example.aiedabackend.vo.FileListResponseVO;
import org.example.aiedabackend.vo.FilePreviewVO;
import org.example.aiedabackend.vo.FileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRepository fileRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String LLM_SERVICE_BASE_URL = "http://localhost:8000/api/v1/user";

    @Override
    public FileVO uploadFile(MultipartFile file, String uid, String sid, String metadata) {
        try {
            System.out.println("FileServiceImpl.uploadFile 被调用，uid: " + uid + ", sid: " + sid);
            
            // 1. 准备上传到大模型服务的请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("user_id", uid);
            body.add("session_id", sid);
            if (metadata != null) {
                body.add("metadata", metadata);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 2. 上传到大模型服务
            String uploadUrl = LLM_SERVICE_BASE_URL + "/upload";
            System.out.println("准备上传到大模型服务，URL: " + uploadUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || !"success".equals(response.getBody().get("status"))) {
                System.out.println("上传到大模型服务失败，响应: " + response.getBody());
                throw new RuntimeException("上传到大模型服务失败: " + response.getBody());
            }

            // 3. 从大模型服务响应中获取文件信息
            Map<String, Object> fileInfo = (Map<String, Object>) response.getBody().get("file_info");
            System.out.println("大模型服务返回的文件信息: " + fileInfo);
            
            // 4. 使用LLM服务返回的文件ID，而不是自己生成
            String fileId = (String) fileInfo.get("file_id");
            System.out.println("使用LLM返回的文件ID: " + fileId);
            
            // 5. 保存文件元数据到数据库
            File fileEntity = new File(
                fileId,
                Integer.parseInt(uid),
                Integer.parseInt(sid),
                (String) fileInfo.get("original_name"),
                (String) fileInfo.get("saved_name"),
                (String) fileInfo.get("file_path"),
                ((Number) fileInfo.get("file_size")).longValue(),
                (String) fileInfo.get("file_type"),
                LocalDateTime.now()
            );
            
            fileRepository.save(fileEntity);
            System.out.println("文件信息已保存到数据库，文件ID: " + fileId);
            
            return fileEntity.toVO();
            
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Resource downloadFile(String fid) {
        System.out.println("FileServiceImpl.downloadFile 被调用，fid: " + fid);
        
        // 1. 从数据库获取文件信息
//        File file = fileRepository.findByFileId(fid);
//        System.out.println("数据库查询结果，file: " + (file != null ? file.getFileId() : "null"));
//
//        if (file == null) {
//            System.out.println("数据库中未找到文件，fid: " + fid);
//            throw new RuntimeException("文件不存在");
//        }
        
        try {
            // 2. 从大模型服务下载文件
            String downloadUrl = LLM_SERVICE_BASE_URL + "/download/" + fid;
            System.out.println("准备请求大模型服务，URL: " + downloadUrl);
            
            ResponseEntity<Resource> response = restTemplate.getForEntity(downloadUrl, Resource.class);
            System.out.println("大模型服务响应状态码: " + response.getStatusCode());
            
            if (response.getStatusCode() != HttpStatus.OK) {
                System.out.println("大模型服务返回非200状态码: " + response.getStatusCode());
                throw new RuntimeException("从大模型服务下载文件失败，状态码: " + response.getStatusCode());
            }
            
            System.out.println("文件下载成功");
            return response.getBody();
            
        } catch (Exception e) {
            System.out.println("下载文件时发生异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        }
    }

    @Override
    public FilePreviewVO previewFile(String fid) {
//        // 1. 从数据库获取文件信息
//        File file = fileRepository.findByFileId(fid);
//        if (file == null) {
//            throw new RuntimeException("文件不存在");
//        }
        
        try {
            System.out.println("FileServiceImpl.previewFile 被调用，fid: " + fid);
            
            // 2. 从大模型服务获取预览
            String previewUrl = LLM_SERVICE_BASE_URL + "/preview/" + fid;
            System.out.println("请求大模型服务预览URL: " + previewUrl);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(previewUrl, Map.class);
            System.out.println("大模型服务响应状态: " + response.getStatusCode());
            System.out.println("大模型服务响应内容: " + response.getBody());
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("大模型服务响应错误，状态码: " + response.getStatusCode());
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("大模型服务返回空响应");
            }
            
            if (!"success".equals(responseBody.get("status"))) {
                String errorMessage = (String) responseBody.get("message");
                throw new RuntimeException("大模型服务预览失败: " + (errorMessage != null ? errorMessage : "未知错误"));
            }
            
            // 3. 转换响应数据
            FilePreviewVO previewVO = new FilePreviewVO();
            previewVO.setFileId(fid);
            previewVO.setFileName((String) responseBody.get("file_name"));
            
            // 安全地转换文件大小
            Object fileSizeObj = responseBody.get("file_size");
            if (fileSizeObj instanceof Number) {
                previewVO.setFileSize(((Number) fileSizeObj).longValue());
            } else {
                previewVO.setFileSize(0L);
            }
            
            previewVO.setContentType((String) responseBody.get("content_type"));
            previewVO.setPreviewContent((String) responseBody.get("preview_content"));
            
            // 安全地转换截断标志
            Object truncatedObj = responseBody.get("is_truncated");
            if (truncatedObj instanceof Boolean) {
                previewVO.setIsTruncated((Boolean) truncatedObj);
            } else {
                previewVO.setIsTruncated(false);
            }
            
            System.out.println("预览转换成功，内容长度: " + 
                (previewVO.getPreviewContent() != null ? previewVO.getPreviewContent().length() : 0));
            
            return previewVO;
            
        } catch (Exception e) {
            System.out.println("预览文件失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("预览文件失败: " + e.getMessage());
        }
    }

    @Override
    public FileListResponseVO getUserFiles(String uid, String sid) {
        List<File> files;
        
        try {
            Integer uidInt = Integer.parseInt(uid);
            
            if (sid != null && !sid.isEmpty()) {
                Integer sidInt = Integer.parseInt(sid);
                files = fileRepository.findByUidAndSid(uidInt, sidInt);
            } else {
                files = fileRepository.findByUid(uidInt);
            }
            
        } catch (NumberFormatException e) {
            throw new RuntimeException("用户ID或会话ID格式错误");
        }
        
        List<FileVO> fileVOs = files.stream()
                .map(File::toVO)
                .collect(Collectors.toList());
        
        FileListResponseVO response = new FileListResponseVO();
        response.setFiles(fileVOs);
        response.setTotalCount(fileVOs.size());
        
        return response;
    }

    @Override
    public FileListResponseVO getUnlinkedFiles(String uid, String sid) {
        List<File> files;
        
        try {
            Integer uidInt = Integer.parseInt(uid);
            Integer sidInt = Integer.parseInt(sid);
            
            files = fileRepository.findByUidAndSidAndRidIsNull(uidInt, sidInt);
            
        } catch (NumberFormatException e) {
            throw new RuntimeException("用户ID或会话ID格式错误");
        }
        
        List<FileVO> fileVOs = files.stream()
                .map(File::toVO)
                .collect(Collectors.toList());
        
        FileListResponseVO response = new FileListResponseVO();
        response.setFiles(fileVOs);
        response.setTotalCount(fileVOs.size());
        
        return response;
    }

    @Override
    public FileListResponseVO getFilesByRecordId(Integer rid) {
        List<File> files = fileRepository.findByRid(rid);
        
        List<FileVO> fileVOs = files.stream()
                .map(File::toVO)
                .collect(Collectors.toList());
        
        FileListResponseVO response = new FileListResponseVO();
        response.setFiles(fileVOs);
        response.setTotalCount(fileVOs.size());
        
        return response;
    }

    @Override
    public FileVO getFileInfo(String fid) {
        // 首先尝试从数据库查找（主要用于uploads文件夹的文件）
        File file = fileRepository.findByFileId(fid);
        if (file != null) {
            return file.toVO();
        }
        
        // 如果数据库中没有，说明可能是LLM生成的文件，直接创建一个基本的FileVO
        // 这些文件的详细信息将通过其他接口（如预览、下载）从LLM服务获取
        FileVO fileVO = new FileVO();
        fileVO.setFileId(fid);
        fileVO.setOriginalName("未知文件"); // 可以通过其他方式获取文件名
        fileVO.setSavedName("未知文件");
        fileVO.setFilePath("");
        fileVO.setFileSize(0L);
        fileVO.setFileType("application/octet-stream");
        fileVO.setUploadTime(LocalDateTime.now()); // 使用当前时间作为默认值
        fileVO.setDownloadUrl(LLM_SERVICE_BASE_URL + "/download/" + fid);
        
        return fileVO;
    }

    @Override
    public String getDownloadUrl(String fid) {
        // 1. 检查文件是否存在于数据库
        File file = fileRepository.findByFileId(fid);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 2. 返回大模型服务的下载URL
        return LLM_SERVICE_BASE_URL + "/download/" + fid;
    }

    @Override
    public String getPreviewUrl(String fid) {
        // 1. 检查文件是否存在于数据库
        File file = fileRepository.findByFileId(fid);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 2. 返回大模型服务的预览URL
        return LLM_SERVICE_BASE_URL + "/preview/" + fid;
    }

    @Override
    public boolean deleteFile(String fid) {
        try {
            // 1. 从数据库获取文件信息
            File file = fileRepository.findByFileId(fid);
            if (file == null) {
                return false;
            }
            
            // 2. 从大模型服务删除文件（如果大模型服务支持删除接口）
            // 注意：这里假设大模型服务提供了删除接口，如果没有可以省略这一步
            try {
                String deleteUrl = LLM_SERVICE_BASE_URL + "/files/" + fid;
                HttpHeaders headers = new HttpHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);
                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Map.class);
            } catch (Exception e) {
                // 如果大模型服务不支持删除或删除失败，记录日志但不影响数据库操作
                System.out.println("从大模型服务删除文件失败: " + e.getMessage());
            }
            
            // 3. 从数据库删除记录
            fileRepository.delete(file);
            
            return true;
        } catch (Exception e) {
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public FileListResponseVO getFileStructureFromLLM(String uid, String sid) {
        try {
            System.out.println("FileServiceImpl.getFileStructureFromLLM 被调用，uid: " + uid + ", sid: " + sid);
            
            // 调用大模型的文件列表接口
            String listUrl = LLM_SERVICE_BASE_URL + "/list/" + uid + "/" + sid;
            System.out.println("请求大模型文件列表URL: " + listUrl);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(listUrl, Map.class);
            System.out.println("大模型服务响应状态: " + response.getStatusCode());
            System.out.println("大模型服务响应内容: " + response.getBody());
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("大模型服务响应错误，状态码: " + response.getStatusCode());
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !"success".equals(responseBody.get("status"))) {
                throw new RuntimeException("大模型服务返回失败响应");
            }
            
            // 解析文件结构
            Map<String, Object> fileStructure = (Map<String, Object>) responseBody.get("file_structure");
            if (fileStructure == null) {
                FileListResponseVO emptyResponse = new FileListResponseVO();
                emptyResponse.setFiles(new ArrayList<>());
                emptyResponse.setTotalCount(0);
                return emptyResponse;
            }
            
            Map<String, Object> uploads = (Map<String, Object>) fileStructure.get("uploads");
            List<FileVO> fileVOs = new ArrayList<>();
            
            // 遍历uploads目录下的文件
            for (Map.Entry<String, Object> entry : uploads.entrySet()) {
                String filename = entry.getKey();
                Map<String, Object> fileInfo = (Map<String, Object>) entry.getValue();
                
                String fileId = (String) fileInfo.get("file_id");
                String downloadUrl = (String) fileInfo.get("url");
                
                // 构造本地文件路径
                String localPath = "app/workspace/" + uid + "/" + sid + "/uploads/" + filename;
                
                // 创建FileVO对象
                FileVO fileVO = new FileVO();
                fileVO.setFileId(fileId);
                fileVO.setOriginalName(filename);
                fileVO.setSavedName(filename);
                fileVO.setFilePath(localPath);
                fileVO.setDownloadUrl(downloadUrl);
                fileVO.setUserId(uid);
                fileVO.setSessionId(sid);
                
                // 尝试从文件名推断文件类型和大小（如果需要的话）
                // 这里可以根据需要添加更多的文件信息处理逻辑
                
                fileVOs.add(fileVO);
            }
            
            // 构造响应
            FileListResponseVO responseVO = new FileListResponseVO();
            responseVO.setFiles(fileVOs);
            responseVO.setTotalCount(fileVOs.size());
            
            System.out.println("解析完成，共找到 " + fileVOs.size() + " 个文件");
            return responseVO;
            
        } catch (Exception e) {
            System.out.println("获取文件结构失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取文件结构失败: " + e.getMessage());
        }
    }

    @Override
    public Resource getFileByLocalPath(String uid, String sid, String filename) {
        try {
            System.out.println("FileServiceImpl.getFileByLocalPath 被调用，uid: " + uid + ", sid: " + sid + ", filename: " + filename);
            
            // 构造文件的本地路径
            String localPath = "app/workspace/" + uid + "/" + sid + "/uploads/" + filename;
            System.out.println("尝试访问本地文件路径: " + localPath);
            
            // 创建Path对象
            Path filePath = Paths.get(localPath);
            
            // 检查文件是否存在
            if (!filePath.toFile().exists()) {
                System.out.println("文件不存在: " + localPath);
                throw new RuntimeException("文件不存在: " + filename);
            }
            
            // 检查是否是文件（不是目录）
            if (!filePath.toFile().isFile()) {
                System.out.println("路径不是文件: " + localPath);
                throw new RuntimeException("路径不是文件: " + filename);
            }
            
            // 创建Resource对象
            Resource resource = new FileSystemResource(filePath);
            
            System.out.println("本地文件访问成功: " + filename);
            return resource;
            
        } catch (Exception e) {
            System.out.println("本地文件访问失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("本地文件访问失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getHierarchicalFileStructure(String uid, String sid) {
        try {
            System.out.println("FileServiceImpl.getHierarchicalFileStructure 被调用，uid: " + uid + ", sid: " + sid);
            
            // 调用大模型的文件列表接口
            String listUrl = LLM_SERVICE_BASE_URL + "/list/" + uid + "/" + sid;
            System.out.println("请求大模型文件列表URL: " + listUrl);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(listUrl, Map.class);
            System.out.println("大模型服务响应状态: " + response.getStatusCode());
            System.out.println("大模型服务响应内容: " + response.getBody());
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("大模型服务响应错误，状态码: " + response.getStatusCode());
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !"success".equals(responseBody.get("status"))) {
                throw new RuntimeException("大模型服务返回失败响应");
            }
            
            // 直接返回大模型的 file_structure，让前端自己解析
            Map<String, Object> fileStructure = (Map<String, Object>) responseBody.get("file_structure");
            if (fileStructure == null) {
                fileStructure = new HashMap<>();
            }
            
            System.out.println("直接返回大模型文件结构，节点数量: " + fileStructure.size());
            return fileStructure;
            
        } catch (Exception e) {
            System.out.println("获取层次化文件结构失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取层次化文件结构失败: " + e.getMessage());
        }
    }
}
