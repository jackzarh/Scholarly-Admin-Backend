package org.niit_project.backend.service;

import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.repository.BatchRepository;
import org.niit_project.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentRepository studentRepository;

    public Batch createBatch(Batch batch) throws Exception{
        batch.setId(null);
        if(batch.getBatchName() == null){
            throw new Exception("Batch Name cannot be null");
        }

        if(batch.getCourse() == null){
            throw new Exception("Course Cannot be null");
        }

        if(batch.getStartPeriod() == null){
            throw new Exception("Start Period cannot be null");
        }

        if(batch.getEndPeriod() == null){
            throw new Exception("End Period cannot be null");
        }

        if(batch.getAdmin() == null){
            throw new Exception("Admin cannot be null");
        }

        var admin = adminService.getAdmin(batch.getAdmin().toString());
        if(admin.isEmpty()){
            throw new Exception("Admin doesn't exist");
        }


        return batchRepository.save(batch);
    }

    public Batch getCompactBatch(String id) throws Exception{
        var batchExists = batchRepository.findById(id);

        if(batchExists.isEmpty()){
            throw new Exception("Batch doesn't exist");
        }

        var compactBatch = batchExists.get();

        // TODO: -- Work on aggregation to get students between time frames

//        var match Aggregation.match(Crite)

        compactBatch.setCandidates(List.of());

        return compactBatch;

    }

    public List<Batch> getAllBatches(){
        return batchRepository.findAll(Sort.by(Sort.Direction.DESC, "startPeriod"));
    }


}
