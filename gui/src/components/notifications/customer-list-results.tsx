import { useState } from "react";
import PerfectScrollbar from "react-perfect-scrollbar";
import PropTypes from "prop-types";
import { format } from "date-fns";
import NextLink from "next/link";
import {
  Avatar,
  Box,
  Card,
  Checkbox,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
  IconButton,
} from "@mui/material";
import { getInitials } from "../../utils/get-initials";
import React from "react";
import { v4 as uuid } from "uuid";
import { PencilAlt as PencilAltIcon } from "../../icons/pencil-alt";
import MapRoundedIcon from "@mui/icons-material/MapRounded";
import { ConstructionOutlined } from "@mui/icons-material";

export const CustomerListResults = ({
  customers,
  allTabNotifications,
  notificationsCount,
  selectedNotifications,
  onSelectedItemsChanged,
  onPageChange,
  onRowsPerPageChange,
  page,
  rowsPerPage,
}) => {
  const handleSelectAll = (event) => {
    let newSelectedCustomerIds: string[] = [];
    if (notificationsCount === 0) return;
    if (event.target.checked) {
      newSelectedCustomerIds = allTabNotifications.map((customer) => customer.key);
    } else {
      newSelectedCustomerIds = [];
    }

    onSelectedItemsChanged(newSelectedCustomerIds);
  };

  const handleSelectOne = (event, notificationId: string) => {
    if (!selectedNotifications.includes(notificationId)) {
      onSelectedItemsChanged((prevSelected: string[]) => [...prevSelected, notificationId]);
    } else {
      onSelectedItemsChanged((prevSelected: string[]) =>
        prevSelected.filter((key: string) => key !== notificationId)
      );
    }
  };

  return (
    <Card>
      <PerfectScrollbar>
        <Box sx={{ minWidth: 1050 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox">
                  <Checkbox
                    checked={
                      selectedNotifications.length === notificationsCount &&
                      selectedNotifications.length
                    }
                    color="primary"
                    indeterminate={
                      selectedNotifications.length > 0 &&
                      selectedNotifications.length < notificationsCount
                    }
                    onChange={handleSelectAll}
                  />
                </TableCell>
                <TableCell>Notification Type</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {customers.map((customer: MessageMonitor.Notification) => {
                const isNotificationSelected =
                  [...selectedNotifications].indexOf(customer.key) !== -1;
                console.log(customer);
                return (
                  <TableRow
                    hover
                    key={customer.key}
                    selected={[...selectedNotifications].indexOf(customer.key) !== -1}
                  >
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={isNotificationSelected}
                        onChange={(event) => handleSelectOne(event, customer.key)}
                        value="true"
                      />
                    </TableCell>
                    <TableCell>
                      <Box
                        sx={{
                          alignItems: "center",
                          display: "flex",
                        }}
                      >
                        <Typography color="textPrimary" variant="body1">
                          {customer.notificationType}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      {format(customer.notificationGeneratedAt, "dd/MM/yyyy HH:mm:ss")}
                    </TableCell>
                    <TableCell>{customer.notificationText}</TableCell>
                    <TableCell align="right">
                      {/* <NextLink href={`/notifications/${customer.notificationType}`} passHref>
                        <IconButton component="a">
                          <MapRoundedIcon fontSize="medium" />
                        </IconButton>
                      </NextLink> */}
                      <NextLink href={`/map/${customer.key}`} passHref>
                        <IconButton component="a">
                          <MapRoundedIcon fontSize="medium" />
                        </IconButton>
                      </NextLink>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Box>
      </PerfectScrollbar>
      <TablePagination
        component="div"
        count={notificationsCount}
        onPageChange={onPageChange}
        onRowsPerPageChange={onRowsPerPageChange}
        page={page}
        rowsPerPage={rowsPerPage}
        rowsPerPageOptions={[5, 10, 25]}
      />
    </Card>
  );
};

CustomerListResults.propTypes = {
  customers: PropTypes.array.isRequired,
  onSelectedItemsChanged: PropTypes.func,
};
