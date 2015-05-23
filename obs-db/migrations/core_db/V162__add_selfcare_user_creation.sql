Insert ignore into m_role values(0,'selfcare','selfcare only');
Create table if not exists del_temp (permission_name varchar(50));
Insert ignore into del_temp values ('CHANGEPLAN_ORDER');
Insert ignore into del_temp values ('CREATE_ALLOCATION');
Insert ignore into del_temp values ('CREATE_CLIENT');
Insert ignore into del_temp values ('CREATE_MEDIAASSET');
Insert ignore into del_temp values ('CREATE_ORDER');
Insert ignore into del_temp values ('CREATE_PAYMENT');
Insert ignore into del_temp values ('CREATE_SELFCARE');
Insert ignore into del_temp values ('CREATE_TICKET');
Insert ignore into del_temp values ('EMAILVERIFICATION_SELFCARE');
Insert ignore into del_temp values ('GENERATENEWPASSWORD_SELFCARE');
Insert ignore into del_temp values ('ONLINE_PAYMENTGATEWAY');
Insert ignore into del_temp values ('READ_ADDRESS');
Insert ignore into del_temp values ('READ_ALLOCATION');
Insert ignore into del_temp values ('READ_BILLMASTER');
Insert ignore into del_temp values ('READ_CLIENT');
Insert ignore into del_temp values ('READ_FINANCIALTRANSACTIONS');
Insert ignore into del_temp values ('READ_MEDIAASSET');
Insert ignore into del_temp values ('READ_ORDER');
Insert ignore into del_temp values ('READ_PAYMENTGATEWAYCONFIG');
Insert ignore into del_temp values ('READ_PRICE');
Insert ignore into del_temp values ('READ_SELFCARE');
Insert ignore into del_temp values ('READ_TICKET');
Insert ignore into del_temp values ('READ_USER');
Insert ignore into del_temp values ('REGISTER_SELFCARE');
Insert ignore into del_temp values ('RENEWAL_ORDER');
Insert ignore into del_temp values ('SELFREGISTRATION_ACTIVATE');
Insert ignore into del_temp values ('UPDATE_ORDER');
Insert ignore into del_temp values ('UPDATE_SELFCARE');
Insert ignore into del_temp values ('READ_CLIENTIDENTIFIER');
Insert ignore into del_temp values ('DISCONNECT_ORDER');

Insert ignore into m_role_permission 
Select (Select id from m_role where name='selfcare') as rid,b.id from del_temp a,m_permission b where a.permission_name=b.code;
insert ignore into m_appuser values (0,0,1,Null,'selfcare','selfcare', 'only','550add1dc52df6c0e8269ffebff9536e343642979c692df6833a243023be6edc','selfcare@obs.com',0,1,1,1,1);
Insert ignore into m_appuser_role values ((Select id from m_appuser where username='selfcare'),(Select id from m_role where name='selfcare'));
                                                               
