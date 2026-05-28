USE dodominimart;

-- Banner Management
INSERT IGNORE INTO sys_menu VALUES(2080, 'Banner Management', 2000, 9,  '/mall/banner',  'C', '0', 'mall:banner:view',   'fa fa-picture-o',   'admin', now(), 'admin', now(), 'Home banner management');
INSERT IGNORE INTO sys_menu VALUES(2081, 'Banner Query',      2080, 1, '#', 'F', '0', 'mall:banner:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2082, 'Banner Add',        2080, 2, '#', 'F', '0', 'mall:banner:add',    '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2083, 'Banner Edit',       2080, 3, '#', 'F', '0', 'mall:banner:edit',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2084, 'Banner Delete',     2080, 4, '#', 'F', '0', 'mall:banner:remove', '#', 'admin', now(), 'admin', now(), '');

-- Review Management
INSERT IGNORE INTO sys_menu VALUES(2085, 'Review Management', 2000, 10, '/mall/review',  'C', '0', 'mall:review:view',  'fa fa-star-o',      'admin', now(), 'admin', now(), 'Product review moderation');
INSERT IGNORE INTO sys_menu VALUES(2086, 'Review Query',      2085, 1, '#', 'F', '0', 'mall:review:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2087, 'Review Delete',     2085, 2, '#', 'F', '0', 'mall:review:remove', '#', 'admin', now(), 'admin', now(), '');

-- Points Log
INSERT IGNORE INTO sys_menu VALUES(2090, 'Points Log',        2000, 11, '/mall/points',  'C', '0', 'mall:points:view',  'fa fa-diamond',     'admin', now(), 'admin', now(), 'Member points history');
INSERT IGNORE INTO sys_menu VALUES(2091, 'Points Query',      2090, 1, '#', 'F', '0', 'mall:points:list',  '#', 'admin', now(), 'admin', now(), '');

-- Payment Records
INSERT IGNORE INTO sys_menu VALUES(2095, 'Payment Records',   2000, 12, '/mall/payment', 'C', '0', 'mall:payment:view', 'fa fa-credit-card', 'admin', now(), 'admin', now(), 'GCash payment records');
INSERT IGNORE INTO sys_menu VALUES(2096, 'Payment Query',     2095, 1, '#', 'F', '0', 'mall:payment:list', '#', 'admin', now(), 'admin', now(), '');
