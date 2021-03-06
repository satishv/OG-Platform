/*
 * @copyright 2009 - present by OpenGamma Inc
 * @license See distribution for license
 */
$.register_module({
    name: 'og.common.gadgets.trades',
    dependencies: ['og.common.util.ui.dialog'],
    obj: function () {
        var ui = og.common.util.ui, api = og.api,
            template_data, original_config_object,
            dependencies = ['id', 'node', 'version'],
            html = {}, action = {}, $add_trades,
            load, reload, attach_trades_link, format_trades,
            form_save, generate_form_function, form_handler;
        /*
         * Helper functions
         */
        generate_form_function = function (load_handler) {
            return function (css_class) {
                $(css_class).html('Loading form...');
                var form = new og.common.util.ui.Form({
                    selector: css_class, data: {}, module: 'og.views.forms.add-trades',
                    handlers: [{type: 'form:load', handler: function () {load_handler();}}]
                });
                form.children = [new form.Field({
                    module: 'og.views.forms.currency',
                    generator: function (handler, template) {handler(template);}
                })];
                form.dom();
            };
        };
        form_save = function (deal_attributes, trade_id) {
            var obj = {}, user_attributes = {}, has_user_attr;
            $(this).find('[name]').each(function (i, elm) {obj[$(elm).attr('name')] = $(elm).val();});
            delete obj.attr_key;
            delete obj.attr_val;
            $(this).find('.og-awesome-list li').each(function (i, elm) {
                var arr = $(elm).text().split(' = ');
                user_attributes[arr[0]] = arr[1];
            });
            // add attributes
            has_user_attr = Object.keys(user_attributes)[0];
            if (has_user_attr || deal_attributes) obj.attributes = {};
            if (has_user_attr) obj.attributes.userAttributes = user_attributes;
            if (deal_attributes) obj.attributes.dealAttributes = deal_attributes;
            if (!trade_id) action.add(obj);
            else action.replace(obj, trade_id);
            $(this).dialog('close');
        };
        attach_trades_link = function (selector) {
            $(selector).append('<a href="#" class="OG-link-add">add trade</a>').find('.OG-link-add').css({
                'position': 'relative', 'left': '2px', 'top': '3px', 'float': 'left'
            }).unbind('click').bind('click', function (e) {
                e.preventDefault();
                ui.dialog({
                    type: 'input', title: 'Add New Trade', width: 400, height: 560,
                    form: generate_form_function(form_handler),
                    buttons: {
                        'Create': form_save.partial(null),
                        'Cancel': function () {$(this).dialog('close');}
                    }
                });
            });
        };
        form_handler = function (trade_obj) {
            var populate_form_fields, attach_calendar, activate_user_attributes_link, activate_user_attributes_delete;
            $add_trades = $('.OG-js-add-trades');
            populate_form_fields = function (trade_obj) {
                var user_attributes_list = [], key, has = 'hasOwnProperty';
                $add_trades.find('[name]').each(function (i, val) {
                    // special case 'premium' as there are two fields for the one value
                    var attribute = $(val).attr('name'), value = trade_obj.premium.split(' ');
                    if (attribute === 'premium') trade_obj.premium = value[0], trade_obj.currency = value[1];
                    if (attribute === 'counterParty') trade_obj.counterParty = trade_obj.counterParty.split('~')[1];
                    $(val).val(trade_obj[attribute]);
                });
                // user attributes
                if (trade_obj.attributes && trade_obj.attributes.userAttributes) {
                    for (key in trade_obj.attributes.userAttributes) {
                        if (trade_obj.attributes.userAttributes[has](key)) {
                            user_attributes_list.push(html.user_attribute
                                .replace('{KEY}', key)
                                .replace('{VALUE}', trade_obj.attributes.userAttributes[key])
                            );
                        }
                    }
                }
                $add_trades.find('.og-awesome-list').html(user_attributes_list.join(''));
            };
            attach_calendar = function () {
                $('.OG-js-datetimepicker').datetimepicker({
                    firstDay: 1, showTimezone: true, dateFormat: 'yy-mm-dd',timeFormat: 'hh:mm ttz'
                });
                $add_trades.find('.og-inline-form').click(function (e) {
                    e.preventDefault();
                    $(this).prev().find('input').datetimepicker('setDate', new Date());
                });
            };
            activate_user_attributes_link = function () {
                $add_trades.find('.og-js-add-attribute').click(function (e) {
                    e.preventDefault();
                    if (!$add_trades.find('[name=attr_key]').val() || !$add_trades.find('[name=attr_val]').val()) {
                        return;
                    }
                    $add_trades.find('.og-awesome-list').prepend(html.user_attribute
                        .replace('{KEY}', $add_trades.find('[name=attr_key]').val())
                        .replace('{VALUE}', $add_trades.find('[name=attr_val]').val())
                    );
                    $add_trades.find('[name^=attr]').val('');
                    activate_user_attributes_delete();
                });
            };
            activate_user_attributes_delete = function () {
                $add_trades.find('.og-js-rem').unbind('click').click(function (e) {$(e.target).parent().remove();});
            };
            if (trade_obj) populate_form_fields(trade_obj);
            attach_calendar(), activate_user_attributes_link(), activate_user_attributes_delete();
        };
        /*
         * Formats arrays of trade objects for submission.
         * The object that we receive in the response can't be sent back as is because it's been formatted slightly
         * differently, this also applies for the form object for the new trade to be added
         */
        format_trades = function (trades) {
            return (trades || []).map(function (trade) {
                var premium, tradeDate;
                if (trade.premium) {
                    premium = trade.premium.toString().split(' ');
                    trade.premium = premium[0].replace(/[,.]/g, '');
                    if (premium[1]) trade.premiumCurrency = premium[1];
                } else delete trade.premium;
                if (trade.premium_date_time) {
                    premium = trade.premium_date_time.split(' ');
                    trade.premiumDate = premium[0];
                    if (premium[1]) trade.premiumTime = premium[1];
                    if (premium[2]) trade.premiumOffset = premium[2].replace(/\((.*)\)/, '$1');
                }
                if (trade.trade_date_time) {
                    tradeDate = trade.trade_date_time.split(' ');
                    trade.tradeDate = tradeDate[0];
                    if (tradeDate[1]) trade.tradeTime = tradeDate[1];
                    if (tradeDate[2]) {
                        trade.tradeOffset = tradeDate[2].replace(/\((.*)\)/, '$1');
                        trade.tradeOffset.toString();
                    }
                }
                if (trade.counterParty) trade.counterParty =
                    trade.counterParty.split('~')[1] || trade.counterParty;
                if (trade.quantity) trade.quantity = trade.quantity.replace(/[,.]/g, '');
                if (trade.currency) trade.premiumCurrency = trade.currency, delete trade.currency;
                delete trade.premium_date_time,
                delete trade.trade_date_time,
                delete trade.id;
                return trade;
            });
        };
        /*
         * Templates for rendering trades table
         */
        html.og_table = '\
          <table class="OG-table">\
            <thead>\
              <tr>\
                <th><span>Trades</span></th>\
                <th>Quantity</th>\
                <th>Counterparty</th>\
                <th>Trade Date / Time</th>\
                <th>Premium</th>\
                <th>Premium Date / Time</th>\
              </tr>\
            </thead>\
            <tbody>{TBODY}</tbody>\
          </table>';
        html.attributes = '\
          <tr class="og-js-attribute" style="display: none">\
            <td colspan="6" style="padding: 0 10px 10px 24px; position: relative">\
              <table class="og-sub-list">{TBODY}</table>\
            </td>\
          </tr>\
        ';
        html.sub_header = '<tbody><tr><td class="og-header" colspan="2">{ATTRIBUTES}</td></tr></tbody>',
        html.user_attribute = '<li><div class="og-del og-js-rem"></div>{KEY} = {VALUE}</li>';
        /*
         * CRUD operations
         */
        action.add = function (trade) {
            var handler = function (result) {
                if (result.error) return alert(result.message);
                var trades = result.data.trades || [];
                trades.push(trade);
                action.put(format_trades(trades));
            };
            api.rest.positions.get({
                dependencies: dependencies,
                id: template_data.object_id,
                handler: handler
            });
        };
        action.replace = function (trade, trade_id) {
            var handler = function (result) {
                if (result.error) return alert(result.message);
                var trades = result.data.trades || [];
                trades.forEach(function (trade, i) {
                    if (trade.id.split('~')[1] === trade_id) {trades.splice(i, 1);}
                });
                trades.push(trade);
                action.put(format_trades(trades));
            };
            api.rest.positions.get({
                dependencies: dependencies,
                id: template_data.object_id,
                handler: handler
            });
        };
        action.del = function (trade_id) {
            var handler = function (result) {
                if (result.error) return alert(result.message);
                var trades = result.data.trades;
                trades.forEach(function (trade, i) {
                    if (trade_id === trade.id.split('~')[1]) trades.splice(i, 1);
                });
                ui.dialog({
                    type: 'confirm',
                    title: 'Delete trade?',
                    width: 400, height: 190,
                    message: 'Are you sure you want to permanently delete trade ' +
                        '<strong style="white-space: nowrap">' + trade_id + '</strong>?',
                    buttons: {
                        'Delete': function () {
                            action.put(format_trades(trades));
                            $(this).dialog('close');
                        },
                        'Cancel': function () {$(this).dialog('close');}
                    }
                });
            };
            api.rest.positions.get({
                dependencies: dependencies,
                id: template_data.object_id,
                handler: handler
            });
        };
        action.edit = function (trade_id) {
            var handler = function (result) {
                var trade_obj, deal_attributes;
                if (result.error) return alert(result.message);
                // get the trade object that you want to edit
                result.data.trades.forEach(function (trade) {
                    if (trade_id === trade.id.split('~')[1]) {trade_obj = trade}
                });
                deal_attributes = (trade_obj.attributes && trade_obj.attributes.dealAttributes)
                    ? trade_obj.attributes.dealAttributes : null;
                ui.dialog({
                    type: 'input', title: 'Edit Trade: ' + trade_id, width: 400, height: 560,
                    form: generate_form_function(form_handler.partial(trade_obj)),
                    buttons: {
                        'Save': form_save.partial(deal_attributes, trade_id),
                        'Save new': form_save.partial(deal_attributes),
                        'Cancel': function () {$(this).dialog('close');}
                    }
                });
            };
            api.rest.positions.get({dependencies: dependencies, id: template_data.object_id, handler: handler});
        };
        action.put = function (trades) {
            api.rest.positions.put({
                trades: trades, id: template_data.object_id, quantity: template_data.quantity,
                handler: function (result) {
                    if (result.error) return ui.dialog({type: 'error', message: result.message});
                }
            });
        };
        load = function (config) {
            var version = config.version !== '*' ? config.version : (void 0), handler = function (result) {
                if (result.error) return alert(result.message);
                original_config_object = config;
                template_data = result.data.template_data;
                var trades = result.data.trades, selector = config.selector, tbody, has_attributes = false,
                    fields = ['id', 'quantity', 'counterParty', 'trade_date_time', 'premium', 'premium_date_time'];
                if (!trades) return $(selector).html(html.og_table.replace('{TBODY}',
                    '<tr><td colspan="6">No Trades</td></tr>')), attach_trades_link(selector);
                tbody = trades.reduce(function (acc, trade) {
                    acc.push('<tr class="og-row"><td>', fields.map(function (field, i) {
                        var expander;
                        i === 0 ? expander = '<span class="OG-icon og-icon-expand"></span>' : expander = '';
                        return expander + (trade[field].replace(/.*~/, '')).lang();
                    }).join('</td><td>'), '</td></tr>');
                    /*
                     * display trade attributes if available
                     */
                    (function () {
                        if (!trade.attributes) return;
                        var attr, attr_type, attr_obj, key, html_arr = [];
                        for (attr_type in trade.attributes) {
                            attr_obj = trade.attributes[attr_type], attr = [];
                            for (key in attr_obj) attr.push(
                                '<tr><td>', key.replace(/.+~(.+)/, '$1').lang(),
                                ':</td><td>', attr_obj[key].lang(), '</td></tr>'
                            );
                            html_arr.push(
                                html.sub_header.replace('{ATTRIBUTES}', attr_type.lang()) +
                                '<tbody class="OG-background-01">' + attr.join('') + '</tbody>'
                            );
                        }
                        acc.push(html.attributes.replace('{TBODY}', html_arr.join('')));
                        if (html_arr.length) has_attributes = true;
                    }());
                    return acc;
                }, []).join('');
                $(selector).html(html.og_table.replace('{TBODY}', tbody)).hide().fadeIn();
                /*
                 * Remove expand links when no trade attributes are available
                 */
                if (!has_attributes) $(config.selector + ' .og-icon-expand').hide();
                $(selector + ' .OG-table > tbody > tr').each(function () {
                    var $this = $(this);
                    if ($this.next().hasClass('og-js-attribute')) {
                        $this.find('.og-icon-expand').unbind('click').bind('click', function (e) {
                            e.stopPropagation();
                            $(this).toggleClass('og-icon-collapse').parents('tr').next().toggle();
                        });
                    } else $this.find('.og-icon-expand').css('visibility', 'hidden');
                });
                if (!version) attach_trades_link(selector);
                $(selector + ' > .OG-table > tbody > tr:not(".og-js-attribute"):last td').css('padding-bottom', '10px');
                $(selector + ' .OG-table').awesometable({height: 400});
                /*
                 * Enable edit/delete trade
                 */
                (function () {
                    if (version) return;
                    var swap_css = function (elm, css) {
                        $(elm).find('td').css(css);
                        if ($(elm).next().hasClass('og-js-attribute')) {
                            $(elm).next().find('> td').css(css);
                        }
                    };
                    $(selector + ' .og-row').hover(
                        function () {
                            swap_css(this, {'background-color': '#d7e7f2', 'cursor': 'default'});
                            $(this).find('td:last-child').append('<div class="og-del"></div>');
                        },
                        function () {
                            swap_css(this, {'background-color': '#ecf5fa'});
                            $(this).find('.og-del').remove();
                        }
                    ).click(function (e) {
                        var trade_id = $(this).find('td:first-child').text();
                        if ($(e.target).is('.og-del')) e.stopPropagation(), action.del(trade_id);
                        else action.edit(trade_id);
                    })
                }());
            };
            api.rest.positions.get({
                dependencies: dependencies, id: config.id, handler: handler, cache_for: 500, version: version
            });
        };
        reload = function () {load(original_config_object);};
        return {render: load, reload: reload, format: format_trades}
    }
});