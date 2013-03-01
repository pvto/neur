DROP VIEW "PUBLIC"."RREC_AVG_ORD";
DROP VIEW "PUBLIC"."run_percentages_2";
DROP TABLE "PUBLIC"."RREC_AVG";
DROP TABLE "PUBLIC"."RREC";
DROP TABLE "PUBLIC"."RUN";
DROP VIEW "PUBLIC"."SAMPLE";
DROP TABLE "PUBLIC"."D_VALIDATION_SET";
DROP TABLE "PUBLIC"."D_DATAITEM";
DROP TABLE "PUBLIC"."D_SPECIMEN";
DROP TABLE "PUBLIC"."D_SAMPLE";
DROP TABLE "PUBLIC"."D_DATASET";


CREATE TABLE "PUBLIC"."D_DATASET"
(
    DATASET_ID bigint primary key,
    NAME varchar(64),
    DESCRIPTION varchar(512)
);
CREATE TABLE "PUBLIC"."D_SAMPLE"
(
    SAMPLE_ID bigint identity,
    DATASET_ID bigint,
    SIZE integer not null,
    constraint FK_DSS_DSID foreign key (DATASET_ID) references D_DATASET (DATASET_ID)
);
alter table d_sample add column name varchar(40);
alter table d_sample alter column name set not null;
alter table d_sample add constraint UQ_DSSA_DSIDNAME unique (DATASET_ID,NAME);

CREATE TABLE "PUBLIC"."D_SPECIMEN"
(
    SPECIMEN_ID bigint identity,
    SAMPLE_ID bigint not null,
    SP_NUMBER int not null,
    constraint FK_SSP_SAID foreign key (SAMPLE_ID) references D_SAMPLE (SAMPLE_ID),
    unique (SAMPLE_ID, SP_NUMBER)
);
CREATE TABLE "PUBLIC"."D_DATAITEM"
(
    SPECIMEN_ID bigint not null,
    INDEX int not null,
    XGROUP int default 0 not null,
    DATA decimal not null,
    constraint FK_SDAI_SPID foreign key (SPECIMEN_ID) references D_SPECIMEN (SPECIMEN_ID),
    unique(SPECIMEN_ID,INDEX,XGROUP)
);
alter table d_dataitem drop column data;
alter table d_dataitem add column data decimal(22,9) default 0;

CREATE TABLE "PUBLIC"."D_VALIDATION_SET"
(
    VALIDATION_SET_ID bigint identity, 
    SPECIMEN_ID bigint,
    constraint FK_DSVS_SPID foreign key (SPECIMEN_ID) references D_SPECIMEN (SPECIMEN_ID)
);

create view sample as
(
    select 
        d.name as dataset, 
        s.name as sample,
        p.sp_number as number,
        i.*,
        d.dataset_id
    from d_dataset d, d_sample s, d_specimen p, d_dataitem i
    where s.dataset_id=d.dataset_id 
    and p.sample_id=s.sample_id
    and i.specimen_id=p.specimen_id
);


CREATE TABLE "PUBLIC"."RUN"
(
   DATE timestamp,
   RUNTIME bigint,
   INLR integer,
   HIDLR integer,
   OUTLR integer,
   SAMPLE integer,
   FUNCTION varchar,
   MAX_ITERS integer,
   ITERS integer,
   TEACH_MODE varchar,
   TESTSET_SIZE integer,
   VLDSET_SIZE integer,
   VLDSET_CORRECT integer,
   TESTSET_CORRECT integer,
   DATA_GEN varchar,
   IMPRV_EPOCHS integer,
   LEARNING_RATE decimal,
   TARG_SD decimal,
   VLDSET_PERCENTAGE decimal,
   TESTSET_PERCENTAGE decimal,
   LOG varchar
)
;
ALTER TABLE "PUBLIC"."RUN" ADD COLUMN rnd_search_iters INT DEFAULT 0;
ALTER TABLE "PUBLIC"."RUN" ADD COLUMN dyn_lrate INT DEFAULT 0;
ALTER TABLE "PUBLIC"."RUN" ADD COLUMN rnd_best_iter INT DEFAULT 0;
ALTER TABLE "PUBLIC"."RUN" ADD COLUMN rnd_search_time BIGINT DEFAULT 0;
ALTER TABLE "PUBLIC"."RUN" ADD COLUMN mlpfile VARCHAR(64);


create view run_percentages_2 as
(
	select data_gen, 
		sample,
		count(data_gen) items, 
		hidlr, 
		avg(testset_correct*100/testset_size) test_corr, 
		avg(vldset_correct*100/vldset_size) vld_corr,
		avg(iters) iters
	  from run
	  group by data_gen,sample,hidlr
);


CREATE TABLE "PUBLIC"."RREC"
(
   DATE timestamp,
   DATA_GEN varchar(14),
   SAMPLE varchar(4),
   AFUNC varchar(32),
   AFUNC_LRC decimal(6,5),
   LALG varchar(32),
   TEACH_MODE varchar(32),
   LRATE decimal(6,5),
   DYN_LRATE integer,
   HID_COUNT integer,
   HID_LR_C integer,
   TRAIN_SIZE integer,
   TEST_SIZE integer,
   TRAIN_OK integer,
   TEST_OK integer,
   SQR_ERROR decimal(22,9),
   STOCH_BEST_ITER integer,
   STOCH_TOT_ITER integer,
   EBP_BEST_ITER integer,
   EBP_TOT_ITER integer,
   STOCH_TIME integer,
   TOT_TIME integer,
    FITNESS decimal(22,9)
);

CREATE TABLE "PUBLIC"."RREC_AVG"
(
   RUN_COUNT integer,
   DATA_GEN varchar(14),
   SAMPLE varchar(4),
   AFUNC varchar(32),
   AFUNC_LRC decimal(6,5),
   LALG varchar(32),
   TEACH_MODE varchar(32),
   LRATE decimal(6,5),
   DYN_LRATE integer,
   HID_COUNT integer,
   HID_LR_C integer,
   TRAIN_SIZE integer,
   TEST_SIZE integer,
   TRAIN_OK decimal(22,9),
    TRAIN_OK_VAR decimal(22,9),
   TEST_OK decimal(22,9),
    TEST_OK_VAR decimal(22,9),
   STOCH_BEST_ITER decimal(22,9),
    STOCH_BEST_ITER_VAR decimal(22,9),
   STOCH_TOT_ITER decimal(22,9),
    STOCH_TOT_ITER_VAR decimal(22,9),
   EBP_BEST_ITER decimal(22,9),
    EBP_BEST_ITER_VAR decimal(22,9),
   EBP_TOT_ITER decimal(22,9),
    EBP_TOT_ITER_var decimal(22,9),
   STOCH_TIME decimal(22,9),
    STOCH_TIME_VAR decimal(22,9),
   TOT_TIME decimal(22,9),
    TOT_TIME_VAR decimal(22,9),
   FITNESS decimal(22,9),
    FITNESS_VAR decimal(22,9)
)
;
ALTER TABLE "PUBLIC"."RREC_AVG" ADD TEST_ERR decimal(5,2);
ALTER TABLE "PUBLIC"."RREC_AVG" ADD TRAIN_ERR decimal(5,2);

create view rrec_avg_ord as
select co.*,av.* from rrec_avg av
left join (select count(*) from rrec_avg) co on 1=1
order by test_ok desc, train_ok desc, hid_count, stoch_time+tot_time
; 
