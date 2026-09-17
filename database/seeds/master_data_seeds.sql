-- Master Data Seeds for Local Development and Demonstrations
-- Module: MF-02 Vaccination Campaign, Center & Session Management
-- Populates baseline approved Vaccine Types and Target Demographic Groups required for campaign creation.

INSERT INTO vaccine_types (code, name, dose_unit, active, created_at, updated_at) VALUES
('COVID19-MRNA', 'COVID-19 mRNA Vaccine', 'dose', TRUE, NOW(6), NOW(6)),
('INFLUENZA-QUAD', 'Seasonal Quadrivalent Influenza Vaccine', 'dose', TRUE, NOW(6), NOW(6)),
('HPV-NONA', 'Human Papillomavirus 9-Valent Vaccine', 'dose', TRUE, NOW(6), NOW(6)),
('MMR', 'Measles, Mumps, and Rubella Vaccine', 'dose', TRUE, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE active = TRUE;

INSERT INTO target_groups (code, description, active, created_at, updated_at) VALUES
('ADULTS-18-PLUS', 'General Adult Population (Ages 18+)', TRUE, NOW(6), NOW(6)),
('ELDERLY-65-PLUS', 'Senior Citizens (Ages 65+)', TRUE, NOW(6), NOW(6)),
('HEALTHCARE-WORKERS', 'Frontline Healthcare Personnel', TRUE, NOW(6), NOW(6)),
('PEDIATRIC-6-17', 'Pediatric and Adolescent Cohort (Ages 6–17)', TRUE, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE active = TRUE;
